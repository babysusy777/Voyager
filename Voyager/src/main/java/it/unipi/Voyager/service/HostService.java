package it.unipi.Voyager.service;

import it.unipi.Voyager.dto.FacilitiesGapDTO;
import it.unipi.Voyager.dto.SeasonalConcentrationDTO;
import it.unipi.Voyager.dto.VisibilityGapDTO;
import it.unipi.Voyager.model.Host;
import it.unipi.Voyager.model.Hotel;
import it.unipi.Voyager.repository.HostRepository;
import it.unipi.Voyager.repository.HotelRepository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HostService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private HostRepository hostRepository;

   /* // VERSIONE CON LOOKUP
    public List<VisibilityGapDTO> getHostVisibilityGap(String username) {

        List<Document> pipeline = Arrays.asList(

                new Document("$match", new Document("username", username)),

                new Document("$unwind", "$hotels"),

                // 3. LOOKUP PEER STATS (Sub-pipeline complessa)
                new Document("$lookup", new Document("from", "hotels")
                        .append("let", new Document("hCity", "$hotels.city")
                                .append("hStars", "$hotels.stars"))
                        .append("pipeline", Arrays.asList(
                                new Document("$match", new Document("$expr",
                                        new Document("$and", Arrays.asList(
                                                new Document("$eq", Arrays.asList("$cityName", "$$hCity")),
                                                new Document("$eq", Arrays.asList("$HotelRating",
                                                        new Document("$switch", new Document("branches", Arrays.asList(
                                                                new Document("case", new Document("$eq", Arrays.asList("$$hStars", 1))).append("then", "oneStar"),
                                                                new Document("case", new Document("$eq", Arrays.asList("$$hStars", 2))).append("then", "twoStar"),
                                                                new Document("case", new Document("$eq", Arrays.asList("$$hStars", 3))).append("then", "threeStar"),
                                                                new Document("case", new Document("$eq", Arrays.asList("$$hStars", 4))).append("then", "fourStar"),
                                                                new Document("case", new Document("$eq", Arrays.asList("$$hStars", 5))).append("then", "fiveStar")
                                                        )).append("default", "Unknown")))
                                                )
                                        ))
                                )),
                                new Document("$group", new Document("_id", null)
                                        .append("avgVisits", new Document("$avg", "$guestStats.totalVisits")))
                        ))
                        .append("as", "peerData")),

                // 4. LOOKUP ACTUAL STATS (Join semplice)
                new Document("$lookup", new Document("from", "hotels")
                        .append("localField", "hotels.hotel_id")
                        .append("foreignField", "_id")
                        .append("as", "actualHotelInfo")),

                // 5. PROJECTION & GAP CALCULATION
                new Document("$project", new Document("hotelName", "$hotels.hotel_name")
                        .append("city", "$hotels.city")
                        .append("category", "$hotels.stars")
                        .append("actualVisits", new Document("$arrayElemAt", Arrays.asList("$actualHotelInfo.guestStats.totalVisits", 0)))
                        .append("averagePeerVisits", new Document("$arrayElemAt", Arrays.asList("$peerData.avgVisits", 0)))
                        .append("gap", new Document("$subtract", Arrays.asList(
                                new Document("$arrayElemAt", Arrays.asList("$actualHotelInfo.guestStats.totalVisits", 0)),
                                new Document("$arrayElemAt", Arrays.asList("$peerData.avgVisits", 0))
                        ))))
        );


        List<Document> results = new ArrayList<>();
        mongoTemplate.getCollection("hosts").aggregate(pipeline).into(results);


        return results.stream()
                .map(doc -> mongoTemplate.getConverter().read(VisibilityGapDTO.class, doc))
                .collect(Collectors.toList());
    }*/

    // VERSIONE CAMPO PRECALCOLATO
    public List<VisibilityGapDTO> getGapSimple(String email) {
        // 1. Recupero l'host (Plain Language)
        Document hostFilter = new Document("email", email);
        Document hostDoc = mongoTemplate.getCollection("hosts").find(hostFilter).first();

        if (hostDoc == null) return Collections.emptyList();

        // 2. Prendo la lista degli hotel referenziati
        List<Document> hotelRefs = hostDoc.getList("hotels", Document.class);
        if (hotelRefs == null) return Collections.emptyList();

        return hotelRefs.stream().map(ref -> {
            // Recuperiamo i dati dal riferimento (Partial Embedding)
            String hotelId = ref.getString("hotel_id");
            if (hotelId == null) return null;

            // 3. Query usando Nome e Città invece dell'ID
            Document hotelDoc = mongoTemplate.getCollection("hotels")
                    .find(new Document("_id", new ObjectId(hotelId)))
                    .first();

            if (hotelDoc == null) {
                System.out.println("DEBUG: Hotel not found for id " + hotelId);
                return null;
            }
            // 4. Estrazione dati dai documenti annidati
            Document stats = (Document) hotelDoc.get("guestStats");

            String hName = hotelDoc.getString("HotelName");
            String hCity = hotelDoc.getString("cityName");
            String category = hotelDoc.getString("HotelRating");

            // Gestione tipi: actualVisits è int nel DTO, cityCategoryAvgVisits è double nel DB
            int totalVisits = (stats != null && stats.get("totalVisits") != null)
                    ? stats.getInteger("totalVisits") : 0;

            double avgVisits = (stats != null && stats.getDouble("city_category_avg_visits") != null)
                    ? stats.getDouble("city_category_avg_visits") : 0.0;

            double gap = totalVisits - avgVisits;

            // 5. Costruttore DTO: ordine sincronizzato con il file DTO
            return new VisibilityGapDTO(
                    hName,        // hotelName
                    hCity,        // city
                    category,    // category
                    totalVisits, // actualVisits (int)
                    avgVisits,   // averagePeerVisits (double)
                    gap          // gap (double)
            );

        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<SeasonalConcentrationDTO> getSeasonalConcentration(String hostEmail) {
        //la gestiamo con linking, dall'host recupero gli id dei suoi hotel e poi faccio l'aggregation sull'hotel

        Host host = hostRepository.findByEmail(hostEmail)
                .orElseThrow(() -> new RuntimeException("Host not found"));

        List<ObjectId> hotelObjectIds = host.getHotels().stream()
                .map(Host.HotelReference::getHotelId)
                .filter(id -> id != null && !id.isEmpty())
                .map(ObjectId::new)
                .toList();

        return hotelRepository.getSeasonalConcentrationByIds(hotelObjectIds);
    }


}