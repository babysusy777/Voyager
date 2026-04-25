package it.unipi.Voyager.service;

import it.unipi.Voyager.dto.FacilitiesGapDTO;
import it.unipi.Voyager.dto.HostHotelUpdateRequest;
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

    public List<VisibilityGapDTO> getGapSimple(String email) {
        // 1. Recupero l'host
        Document hostFilter = new Document("email", email);
        Document hostDoc = mongoTemplate.getCollection("hosts").find(hostFilter).first();

        if (hostDoc == null) return Collections.emptyList();

        // 2. Prendo la lista degli hotel referenziati
        List<Document> hotelRefs = hostDoc.getList("hotels", Document.class);
        if (hotelRefs == null) return Collections.emptyList();

        return hotelRefs.stream().map(ref -> {
            // Recuperiamo i dati dal riferimento (Partial Embedding)
            String hotelId = ref.getString("hotel_id");

            Document hotelDoc = mongoTemplate.getCollection("hotels")
                    .find(new Document("_id", new ObjectId(hotelId)))
                    .first();

            if (hotelDoc != null) {
                Document stats = (Document) hotelDoc.get("guestStats");
                System.out.println("DEBUG totalVisits: " + (stats != null ? stats.get("totalVisits") : "stats null"));
            }

            // 4. Estrazione dati dai documenti annidati
            Document stats = (Document) hotelDoc.get("guestStats");

            String hName = hotelDoc.getString("HotelName");
            String hCity = hotelDoc.getString("cityName");
            String category = hotelDoc.getString("HotelRating");

            // Gestione tipi: actualVisits è int nel DTO, cityCategoryAvgVisits è double nel DB
            int totalVisits = 0;
            if (stats != null && stats.get("totalVisits") != null) {
                Object raw = stats.get("totalVisits");
                if (raw instanceof Integer) totalVisits = (Integer) raw;
                else if (raw instanceof Long) totalVisits = ((Long) raw).intValue();
                else if (raw instanceof Double) totalVisits = ((Double) raw).intValue();
            }

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


        public void removeHotelReferenceFromHost(String email, String hotelId) {
            Host host = hostRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Host not found"));

            if (host.getHotels() != null) {
                // Rimuove l'hotelId corrispondente dalla lista dei riferimenti
                host.getHotels().removeIf(ref -> ref.getHotelId().equals(hotelId));
                hostRepository.save(host);
            }
        }

    public FacilitiesGapDTO getFacilitiesGap(String email, String hotelName, String cityName) {

        Host host = hostRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Host not found"));

        Host.HotelReference targetRef = host.getHotels().stream()
                .filter(h -> h.getHotelName().equalsIgnoreCase(hotelName) &&
                        h.getCity().equalsIgnoreCase(cityName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Hotel not found in your management list"));

        Hotel hotel = hotelRepository.findById(targetRef.getHotelId())
                .orElseThrow(() -> new RuntimeException("Hotel data inconsistency: ID not found in global collection"));

        return hotelRepository.getFacilitiesGap(
                hotel.getCityName(),
                hotel.getHotelRating(),
                hotel.getHotelName(),
                hotel.getFacilities()
        );
    }
    }


