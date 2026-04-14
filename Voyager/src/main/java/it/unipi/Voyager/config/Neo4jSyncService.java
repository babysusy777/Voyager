package it.unipi.Voyager.config;

import it.unipi.Voyager.model.graph.*;
import it.unipi.Voyager.repository.graph.*;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class Neo4jSyncService {

    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private TravellerGraphRepository travellerGraphRepository;
    @Autowired private HotelGraphRepository hotelGraphRepository;
    @Autowired private CityGraphRepository cityGraphRepository;
    @Autowired private AttractionGraphRepository attractionGraphRepository;

    // ─── POPOLAMENTO COMPLETO INIZIALE ────────────────────────────

    public void syncAll() {
        syncCitiesAndAttractions();
        syncHotels();
        syncTravellers();
    }

    // Step A: City + Attraction + IN_CITY
    private void syncCitiesAndAttractions() {
        System.out.println("[Neo4j] Sync City + Attraction...");
        List<Document> cities = new ArrayList<>();
        mongoTemplate.getCollection("cities").find().into(cities);

        for (Document cityDoc : cities) {
            String cityName = cityDoc.getString("cityName");
            if (cityName == null) continue;

            CityNode cityNode = cityGraphRepository.findById(cityName)
                    .orElse(new CityNode());
            cityNode.setCityName(cityName);
            cityGraphRepository.save(cityNode);

            List<Document> attractions = cityDoc.getList("attractions", Document.class);
            if (attractions == null) continue;

            for (Document attrDoc : attractions) {
                String name = attrDoc.getString("name");
                if (name == null) continue;

                List<String> types = attrDoc.getList("type", String.class);
                String category = (types != null && !types.isEmpty()) ? types.get(0) : "unknown";

                AttractionNode attrNode = attractionGraphRepository.findById(name)
                        .orElse(new AttractionNode());
                attrNode.setName(name);
                attrNode.setCategory(category);
                attrNode.setCity(cityNode);
                attractionGraphRepository.save(attrNode);
            }
        }
        System.out.println("[Neo4j] City + Attraction completato.");
    }

    // Step B: Hotel + LOCATED_IN + NEAR_TO
    private void syncHotels() {
        System.out.println("[Neo4j] Sync Hotel...");
        List<Document> hotels = new ArrayList<>();
        mongoTemplate.getCollection("hotels").find().into(hotels);

        for (Document hotelDoc : hotels) {
            String hotelName = hotelDoc.getString("HotelName");
            String cityName  = hotelDoc.getString("cityName");
            String ratingStr = hotelDoc.getString("HotelRating");
            if (hotelName == null) continue;

            int stars = parseStars(ratingStr);

            CityNode cityNode = (cityName != null)
                    ? cityGraphRepository.findById(cityName).orElse(null)
                    : null;

            // NEAR_TO attractions
            List<HotelNearRel> nearRels = new ArrayList<>();
            List<Document> attrDocs = hotelDoc.getList("Attractions", Document.class);
            if (attrDocs != null) {
                for (Document a : attrDocs) {
                    String attrName = a.getString("name");
                    String distStr  = a.getString("distance");
                    if (attrName == null) continue;

                    AttractionNode attrNode = attractionGraphRepository.findById(attrName)
                            .orElse(new AttractionNode());
                    attrNode.setName(attrName);
                    if (attrNode.getCity() == null && cityNode != null) attrNode.setCity(cityNode);
                    attractionGraphRepository.save(attrNode);

                    HotelNearRel rel = new HotelNearRel();
                    rel.setAttraction(attrNode);
                    rel.setDistance(parseDistance(distStr));
                    nearRels.add(rel);
                }
            }

            HotelNode hotelNode = hotelGraphRepository.findByHotelName(hotelName)
                    .orElse(new HotelNode());
            hotelNode.setHotelName(hotelName);
            hotelNode.setStars(stars);
            hotelNode.setCity(cityNode);
            hotelNode.setNearbyAttractions(nearRels);
            hotelGraphRepository.save(hotelNode);
        }
        System.out.println("[Neo4j] Hotel completato.");
    }

    // Step C: Traveller + MADE_TRIP + STAYED_AT
    private void syncTravellers() {
        System.out.println("[Neo4j] Sync Traveller...");
        List<Document> travellers = new ArrayList<>();
        mongoTemplate.getCollection("travellers").find().into(travellers);

        for (Document tDoc : travellers) {
            String email = tDoc.getString("email");
            if (email == null) continue;

            syncTravellerNode(tDoc);
        }
        System.out.println("[Neo4j] Traveller completato.");
    }

    // ─── SYNC SINGOLO TRAVELLER (usato anche da upsertTrip) ───────

    public void syncTravellerNode(Document tDoc) {
        String email = tDoc.getString("email");
        if (email == null) return;

        TravellerNode node = travellerGraphRepository.findById(email)
                .orElse(new TravellerNode());
        node.setEmail(email);
        node.setAge(tDoc.getInteger("age", 0));
        node.setGender(tDoc.getString("gender"));
        node.setUserSegment(tDoc.getString("user_segment"));

        // MADE_TRIP + STAYED_AT
        List<TripNode> tripNodes = new ArrayList<>();
        List<Document> trips = tDoc.getList("past_trips", Document.class);
        if (trips != null) {
            for (Document t : trips) {
                String hotelName = t.getString("hotel_name");

                HotelNode hotelNode = (hotelName != null)
                        ? hotelGraphRepository.findByHotelName(hotelName).orElse(null)
                        : null;

                TripNode tripNode = new TripNode();
                tripNode.setRatingGiven(t.getInteger("rating_given", 0));
                tripNode.setHotel(hotelNode);
                tripNodes.add(tripNode);
            }
        }
        node.setTrips(tripNodes);
        travellerGraphRepository.save(node);
    }

    // Sincronizza un singolo traveller a partire dall'email (per update)
    public void syncTravellerByEmail(String email) {
        Document tDoc = mongoTemplate.getCollection("travellers")
                .find(new Document("email", email)).first();
        if (tDoc != null) syncTravellerNode(tDoc);
    }

    // Sincronizza un singolo hotel a partire dal nome (per update)
    public void syncHotelByName(String hotelName, String cityName) {
        Document hotelDoc = mongoTemplate.getCollection("hotels")
                .find(new Document("HotelName", hotelName).append("cityName", cityName)).first();
        if (hotelDoc == null) return;

        String ratingStr = hotelDoc.getString("HotelRating");
        int stars = parseStars(ratingStr);

        CityNode cityNode = (cityName != null)
                ? cityGraphRepository.findById(cityName).orElse(null)
                : null;

        List<HotelNearRel> nearRels = new ArrayList<>();
        List<Document> attrDocs = hotelDoc.getList("Attractions", Document.class);
        if (attrDocs != null) {
            for (Document a : attrDocs) {
                String attrName = a.getString("name");
                if (attrName == null) continue;
                AttractionNode attrNode = attractionGraphRepository.findById(attrName)
                        .orElse(new AttractionNode());
                attrNode.setName(attrName);
                if (attrNode.getCity() == null && cityNode != null) attrNode.setCity(cityNode);
                attractionGraphRepository.save(attrNode);
                HotelNearRel rel = new HotelNearRel();
                rel.setAttraction(attrNode);
                rel.setDistance(parseDistance(a.getString("distance")));
                nearRels.add(rel);
            }
        }

        HotelNode hotelNode = hotelGraphRepository.findByHotelName(hotelName)
                .orElse(new HotelNode());
        hotelNode.setHotelName(hotelName);
        hotelNode.setStars(stars);
        hotelNode.setCity(cityNode);
        hotelNode.setNearbyAttractions(nearRels);
        hotelGraphRepository.save(hotelNode);
    }

    // ─── UTILITY ──────────────────────────────────────────────────

    private int parseStars(String rating) {
        if (rating == null) return 0;
        return switch (rating) {
            case "OneStar"   -> 1;
            case "TwoStar"   -> 2;
            case "ThreeStar" -> 3;
            case "FourStar"  -> 4;
            case "FiveStar"  -> 5;
            default          -> 0;
        };
    }

    private Double parseDistance(String dist) {
        if (dist == null) return null;
        try {
            return Double.parseDouble(dist.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}