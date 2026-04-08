package it.unipi.Voyager.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unipi.Voyager.model.UserRole;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(1) // Eseguito prima del DatabaseInitializer
public class DataIngestionService {

    @Autowired
    private MongoTemplate mongoTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    @EventListener(ApplicationReadyEvent.class)
    public void ingestAll() {
        ingestHotels();
        ingestHosts();
        ingestTravellers();
        ingestCities();
    }

    // ─── HOTELS ───────────────────────────────────────────────────

    private void ingestHotels() {
        if (mongoTemplate.getCollection("hotels").countDocuments() > 0) {
            System.out.println("[Ingestion] Hotels già presenti, skip.");
            return;
        }
        try {
            InputStream is = new ClassPathResource("hotel_ai_pricing.json").getInputStream();
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
            InputStream is = new ClassPathResource("hosts.json").getInputStream();
            JsonNode root = mapper.readTree(is);
            List<Document> docs = new ArrayList<>();

            for (JsonNode node : root) {
                Document doc = new Document();
                doc.append("username",  node.path("username").asText());
                doc.append("email",     node.path("email").asText());
                doc.append("password",  node.path("password").asText());
                doc.append("full_name", node.path("full_name").asText());
                doc.append("role",      UserRole.HOST.name());

                List<Document> hotels = new ArrayList<>();
                for (JsonNode h : node.path("hotels")) {
                    hotels.add(new Document()
                            .append("hotel_id",   h.path("hotel_id").asText())
                            .append("hotel_name", h.path("hotel_name").asText())
                            .append("city",       h.path("city").asText())
                            .append("stars",      h.path("stars").asInt())
                    );
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
                doc.append("age",      node.path("age").asInt());
                doc.append("country",  node.path("country").asText());
                doc.append("role",     UserRole.TRAVELLER.name());

                JsonNode prefs = node.path("preferences");
                doc.append("preferences", new Document()
                        .append("budget",      prefs.path("budget").asText())
                        .append("travel_type", prefs.path("travel_type").asText())
                        .append("season",      prefs.path("season").asText())
                );

                // past_trips: hotel_id nel JSON diventa hotel_name embedded
                List<Document> trips = new ArrayList<>();
                for (JsonNode t : node.path("past_trips")) {
                    trips.add(new Document()
                            .append("trip_name",    t.path("trip_name").asText())
                            .append("city",         t.path("city").asText())
                            .append("hotel_name",   t.path("hotel_id").asText()) // hotel_id = nome hotel
                            .append("season",       t.path("season").asText())
                            .append("date",         t.path("date").asText())
                            .append("rating_given", t.path("rating_given").asInt())
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
            InputStream is = new ClassPathResource("city_final.json").getInputStream();
            JsonNode root = mapper.readTree(is);
            List<Document> docs = new ArrayList<>();

            for (JsonNode node : root) {
                Document doc = new Document();
                doc.append("cityName", node.path("cityName").asText());

                List<Document> attractions = new ArrayList<>();
                for (JsonNode a : node.path("attractions")) {
                    Document attr = new Document();
                    attr.append("name", a.path("name").asText());
                    List<String> types = new ArrayList<>();
                    for (JsonNode t : a.path("type")) types.add(t.asText());
                    attr.append("type", types);
                    attractions.add(attr);
                }
                doc.append("attractions", attractions);
                docs.add(doc);
            }

            mongoTemplate.getCollection("cities").insertMany(docs);
            System.out.println("[Ingestion] Cities inserite: " + docs.size());

        } catch (Exception e) {
            System.err.println("[Ingestion] Errore cities: " + e.getMessage());
        }
    }
}