package it.unipi.Voyager.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.Voyager.model.UserRole;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

@Component
@Order(1) // Eseguito prima del DatabaseInitializer
public class DataIngestionService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private DatabaseInitializer databaseInitializer;

    private final ObjectMapper mapper = new ObjectMapper();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @EventListener(ApplicationReadyEvent.class)
    public void ingestAll() {
        ingestHotels();
        ingestHosts();
        ingestTravellers();
        ingestCities();
        databaseInitializer.initializeHotelStats();
    }

    // ─── HOTELS ───────────────────────────────────────────────────

    private void ingestHotels() {
        if (mongoTemplate.getCollection("hotels").countDocuments() > 0) {
            System.out.println("[Ingestion] Hotels già presenti, skip.");
            return;
        }
        try {
            // Crea l'indice unique su HotelName + cityName PRIMA dell'inserimento
            mongoTemplate.getCollection("hotels").createIndex(
                    new Document("HotelName", 1).append("cityName", 1),
                    new com.mongodb.client.model.IndexOptions().unique(true)
            );

            InputStream is = new ClassPathResource("hotels.json").getInputStream();
            JsonNode root = mapper.readTree(is);
            List<Document> docs = new ArrayList<>();

            for (JsonNode node : root) {
                Document doc = new Document();
                doc.append("HotelCode",    node.path("HotelCode").asLong());
                doc.append("HotelName",    node.path("HotelName").asText());
                doc.append("cityName",     node.path("cityName").asText());
                doc.append("HotelRating",  node.path("HotelRating").asText());
                doc.append("Address",      node.path("Address").asText());
                doc.append("Description",  node.path("Description").asText());
                doc.append("average_price_per_night", node.path("average_price_per_night").asDouble());
                doc.append("city_cost_tier", node.path("city_cost_tier").asText());

                // HotelFacilities: array nel JSON
                List<String> facilities = new ArrayList<>();
                for (JsonNode f : node.path("HotelFacilities")) {
                    facilities.add(f.asText());
                }
                doc.append("HotelFacilities", facilities);

                // Attractions
                List<Document> attractions = new ArrayList<>();
                for (JsonNode a : node.path("Attractions")) {
                    Document attr = new Document();
                    attr.append("name", a.path("name").asText());
                    attr.append("distance", a.path("distance").asText());
                    List<String> types = new ArrayList<>();
                    for (JsonNode t : a.path("type")) types.add(t.asText());
                    attr.append("type", types);
                    attractions.add(attr);
                }
                doc.append("Attractions", attractions);

                // guestStats inizializzato vuoto — verrà popolato dal DatabaseInitializer
                doc.append("guestStats", new Document()
                        .append("totalVisits", 0)
                        .append("seasonality", new Document()
                                .append("counts", new Document()
                                        .append("spring", 0)
                                        .append("summer", 0)
                                        .append("autumn", 0)
                                        .append("winter", 0)
                                )
                        )
                );

                docs.add(doc);
            }

            mongoTemplate.getCollection("hotels").insertMany(docs);
            System.out.println("[Ingestion] Hotels inseriti: " + docs.size());

        } catch (Exception e) {
            System.err.println("[Ingestion] Errore hotels: " + e.getMessage());
        }
    }

    // ─── HOSTS ────────────────────────────────────────────────────

    private void ingestHosts() {
        if (mongoTemplate.getCollection("hosts").countDocuments() > 0) {
            System.out.println("[Ingestion] Hosts già presenti, skip.");
            return;
        }
        try {
            // Costruisce lookup: "HotelName::cityName" -> MongoDB _id (stringa hex)
            // Usato per linkare gli hotel embedded negli host al documento hotel reale
            Map<String, String> hotelIdLookup = new HashMap<>();
            mongoTemplate.getCollection("hotels")
                    .find()
                    .forEach(h -> {
                        String key = h.getString("HotelName") + "::" + h.getString("cityName");
                        hotelIdLookup.put(key, h.getObjectId("_id").toHexString());
                    });

            InputStream is = new ClassPathResource("hosts.json").getInputStream();
            JsonNode root = mapper.readTree(is);
            List<Document> docs = new ArrayList<>();

            for (JsonNode node : root) {
                Document doc = new Document();
                doc.append("email",     node.path("email").asText());
                doc.append("password",  passwordEncoder.encode(node.path("password").asText()));
                doc.append("full_name", node.path("full_name").asText());
                doc.append("role",      UserRole.HOST.name());

                List<Document> hotels = new ArrayList<>();
                for (JsonNode h : node.path("hotels")) {
                    String hotelName = h.path("hotel_name").asText();
                    String city      = h.path("city").asText();
                    String mongoId   = hotelIdLookup.get(hotelName + "::" + city);

                    Document hotelRef = new Document()
                            .append("hotel_name", hotelName)
                            .append("city",       city)
                            .append("stars",      h.path("stars").asInt());

                    // Collega al documento hotel reale se trovato
                    if (mongoId != null) {
                        hotelRef.append("hotel_id", mongoId);
                    }

                    hotels.add(hotelRef);
                }
                doc.append("hotels", hotels);
                docs.add(doc);
            }

            mongoTemplate.getCollection("hosts").insertMany(docs);
            System.out.println("[Ingestion] Hosts inseriti: " + docs.size());

        } catch (Exception e) {
            System.err.println("[Ingestion] Errore hosts: " + e.getMessage());
        }
    }

    // ─── TRAVELLERS ───────────────────────────────────────────────

    private void ingestTravellers() {
        if (mongoTemplate.getCollection("travellers").countDocuments() > 0) {
            System.out.println("[Ingestion] Travellers già presenti, skip.");
            return;
        }
        try {
            InputStream is = new ClassPathResource("users.json").getInputStream();
            JsonNode root = mapper.readTree(is);
            List<Document> docs = new ArrayList<>();

            for (JsonNode node : root) {
                Document doc = new Document();
                doc.append("user_id",  node.path("user_id").asText());
                doc.append("name",     node.path("name").asText());
                doc.append("gender",   node.path("gender").asText());
                doc.append("email",    node.path("email").asText());
                doc.append("password", passwordEncoder.encode(node.path("password").asText()));
                doc.append("age",      node.path("age").asInt());
                doc.append("country",  node.path("country").asText());
                doc.append("role",     UserRole.TRAVELLER.name());

                JsonNode prefs = node.path("preferences");
                doc.append("preferences", new Document()
                        .append("budget",      prefs.path("budget").asText())
                        .append("travel_type", prefs.path("travel_type").asText())
                        .append("season",      prefs.path("season").asText())
                );

                List<Document> trips = new ArrayList<>();
                for (JsonNode t : node.path("past_trips")) {
                    List<String> cities = new ArrayList<>();
                    cities.add(t.path("city").asText());

                    List<Document> hotelsList = new ArrayList<>();
                    hotelsList.add(new Document()
                            .append("hotelName",  t.path("hotel_id").asText())
                            .append("hotelStars", t.path("hotel_stars").asText())
                    );

                    trips.add(new Document()
                            .append("trip_name",    t.path("trip_name").asText())
                            .append("city",         cities)
                            .append("hotels",       hotelsList)
                            .append("season",       t.path("season").asText())
                            .append("date",         t.path("date").asText())
                            .append("rating_given", t.path("rating_given").asInt())
                            .append("budget",       t.path("trip_budget").asText())
                    );
                }
                doc.append("past_trips", trips);
                docs.add(doc);
            }

            mongoTemplate.getCollection("travellers").insertMany(docs);
            System.out.println("[Ingestion] Travellers inseriti: " + docs.size());

        } catch (Exception e) {
            System.err.println("[Ingestion] Errore travellers: " + e.getMessage());
        }
    }

    // ─── CITIES ───────────────────────────────────────────────────

    private void ingestCities() {
        if (mongoTemplate.getCollection("cities").countDocuments() > 0) {
            System.out.println("[Ingestion] Cities già presenti, skip.");
            return;
        }
        try {
            mongoTemplate.getCollection("cities").createIndex(
                    new Document("cityName", 1),
                    new com.mongodb.client.model.IndexOptions().unique(true)
            );
            System.out.println("[Ingestion] Indice univoco su cityName creato.");

            InputStream is = new ClassPathResource("city.json").getInputStream();
            JsonNode root = mapper.readTree(is);
            List<Document> docs = new ArrayList<>();

            for (JsonNode node : root) {
                Document doc = new Document();
                String cityName = node.path("cityName").asText();

                doc.append("cityName",          cityName);
                doc.append("cost_of_living",    node.path("cost_of_living").asText());
                doc.append("safety",            node.path("safety").asText());
                doc.append("category",          node.path("category").asText());
                doc.append("best_time_to_visit", node.path("best_time_to_visit").asText());


                // 2. Recupero Hotel della città per strategia IBRIDA
                List<Document> allHotelsInCity = new ArrayList<>();
                mongoTemplate.getCollection("hotels")
                        .find(new Document("cityName", cityName))
                        .forEach(allHotelsInCity::add);

                // --- PARTIAL EMBEDDING (20 casuali) ---
                Collections.shuffle(allHotelsInCity);

                List<Document> topValueHotels = new ArrayList<>();
                int limit = Math.min(allHotelsInCity.size(), 20);
                for (int i = 0; i < limit; i++) {
                    Document h = allHotelsInCity.get(i);
                    topValueHotels.add(new Document()
                            .append("hotel_name", h.getString("HotelName"))
                            .append("stars",      h.getString("HotelRating"))
                            .append("avg_price",  h.getDouble("average_price_per_night"))
                    );
                }
                doc.append("top_value_hotels", topValueHotels);

                // --- LINKING (Tutti gli altri ID) ---
                List<String> otherHotelIds = new ArrayList<>();
                for (int i = limit; i < allHotelsInCity.size(); i++) {
                    Document h = allHotelsInCity.get(i);
                    otherHotelIds.add(h.getObjectId("_id").toHexString());
                }
                doc.append("other_hotel_ids", otherHotelIds);

                // 3. Altri campi
                doc.append("city_index", new Document("total_visits", 0).append("hotel_count", allHotelsInCity.size()));

                docs.add(doc);
            }

            mongoTemplate.getCollection("cities").insertMany(docs);
            System.out.println("[Ingestion] Cities inserite con strategia ibrida: " + docs.size());

        } catch (Exception e) {
            System.err.println("[Ingestion] Errore cities: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int parseStars(String rating) {
        if (rating == null) return 0;
        return switch (rating.toLowerCase()) {
            case "onestar"   -> 1;
            case "twostar"   -> 2;
            case "threestar" -> 3;
            case "fourstar"  -> 4;
            case "fivestar"  -> 5;
            default          -> 0;
        };
    }
}