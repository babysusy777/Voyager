package it.unipi.Voyager.config;

import com.mongodb.client.MongoCollection;
import it.unipi.Voyager.repository.graph.TravellerGraphRepository;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Order(2) // Eseguito dopo DataIngestionService
public class DatabaseInitializer {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TravellerGraphRepository travellerNodeRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeHotelStats() {

        // Step 1: totalVisits + seasonality.counts su ogni hotel
        populateGuestStats();

        // Step 2: city_category_avg_visits (dipende da Step 1)
        populateCityCategoryAvgVisits();

        // Step 3: calcola user_segment e lo salva su ogni traveller
        populateTravellerSegments();

        // Step 4: segment_distribution + preference_distribution su ogni hotel (dipende da Step 3)
        populateSegmentAndPreferenceDistribution();
    }

    // ─────────────────────────────────────────────────────────────
    // STEP 1 — totalVisits + seasonality.counts
    // ─────────────────────────────────────────────────────────────
    private void populateGuestStats() {
        System.out.println("[Init] Step 1 — totalVisits e seasonality.counts...");

        MongoCollection<Document> travellers = mongoTemplate.getCollection("travellers");

        List<Document> pipeline = Arrays.asList(

                new Document("$unwind", "$past_trips"),

                new Document("$group", new Document("_id", "$past_trips.hotel_name")
                        .append("totalVisits", new Document("$sum", 1))
                        .append("spring", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.season", "spring")), 1, 0))))
                        .append("summer", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.season", "summer")), 1, 0))))
                        .append("autumn", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.season", "autumn")), 1, 0))))
                        .append("winter", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.season", "winter")), 1, 0))))
                ),

                new Document("$project", new Document("_id", 0)
                        .append("HotelName", "$_id")
                        .append("guestStats", new Document("totalVisits", "$totalVisits")
                                .append("seasonality", new Document("counts", new Document()
                                        .append("spring", "$spring")
                                        .append("summer", "$summer")
                                        .append("autumn", "$autumn")
                                        .append("winter", "$winter")
                                ))
                        )
                ),

                new Document("$merge", new Document("into", "hotels")
                        .append("on", "HotelName")
                        .append("whenMatched", Arrays.asList(
                                new Document("$set", new Document("guestStats", "$$new.guestStats"))
                        ))
                        .append("whenNotMatched", "discard")
                )
        );

        travellers.aggregate(pipeline).toCollection();
        System.out.println("[Init] Step 1 completato.");
    }

    // ─────────────────────────────────────────────────────────────
    // STEP 2 — city_category_avg_visits
    // ─────────────────────────────────────────────────────────────
    private void populateCityCategoryAvgVisits() {
        System.out.println("[Init] Step 2 — city_category_avg_visits...");

        MongoCollection<Document> hotels = mongoTemplate.getCollection("hotels");

        List<Document> pairs = hotels.aggregate(Arrays.asList(
                new Document("$group", new Document("_id",
                        new Document("city", "$cityName").append("rating", "$HotelRating")
                ))
        )).into(new ArrayList<>());

        for (Document pair : pairs) {
            Document id = pair.get("_id", Document.class);
            if (id == null) continue;
            String city   = id.getString("city");
            String rating = id.getString("rating");
            if (city == null || rating == null) continue;

            List<Document> pipeline = Arrays.asList(

                    new Document("$match", new Document("cityName", city)
                            .append("HotelRating", rating)),

                    new Document("$group", new Document("_id", null)
                            .append("peer_avg_visits", new Document("$avg", "$guestStats.totalVisits"))
                            .append("hotels", new Document("$push", "$_id"))),

                    new Document("$unwind", "$hotels"),

                    new Document("$project", new Document("_id", "$hotels")
                            .append("city_avg", "$peer_avg_visits")),

                    new Document("$merge", new Document("into", "hotels")
                            .append("on", "_id")
                            .append("whenMatched", Arrays.asList(
                                    new Document("$set", new Document(
                                            "guestStats.city_category_avg_visits", "$$new.city_avg"
                                    ))
                            ))
                    )
            );

            hotels.aggregate(pipeline).toCollection();
        }

        System.out.println("[Init] Step 2 completato.");
    }

    // ─────────────────────────────────────────────────────────────
    // STEP 3 — user_segment su ogni traveller
    // Stessa logica della query T1, scritta come campo precomputato
    // sul documento traveller per poterla usare nello Step 4.
    // ─────────────────────────────────────────────────────────────
    private void populateTravellerSegments() {
        System.out.println("[Init] Step 3 — user_segment sui travellers...");

        MongoCollection<Document> travellers = mongoTemplate.getCollection("travellers");

        List<Document> pipeline = Arrays.asList(

                new Document("$unwind", "$past_trips"),

                new Document("$group", new Document("_id", "$_id")
                        .append("unique_cities", new Document("$addToSet", "$past_trips.city"))
                        .append("avg_stars",     new Document("$avg", "$past_trips.hotel_stars"))
                        .append("total_trips",   new Document("$sum", 1))
                ),

                // Calcola unique_cities, avg_stars, repeat_ratio
                new Document("$project", new Document()
                        .append("unique_cities_count", new Document("$size", "$unique_cities"))
                        .append("avg_stars", 1)
                        .append("total_trips", 1)
                        // repeat_ratio = 1 - (città uniche / totale trip)
                        // più trip sulla stessa città → ratio più alto
                        .append("repeat_ratio", new Document("$subtract", Arrays.asList(
                                1,
                                new Document("$divide", Arrays.asList(
                                        new Document("$size", "$unique_cities"),
                                        "$total_trips"
                                ))
                        )))
                ),

                // Classifica nel segmento
                new Document("$project", new Document()
                        .append("user_segment", new Document("$switch", new Document("branches", Arrays.asList(
                                new Document("case", new Document("$and", Arrays.asList(
                                        new Document("$gt", Arrays.asList("$unique_cities_count", 4)),
                                        new Document("$lt", Arrays.asList("$repeat_ratio", 0.2))
                                ))).append("then", "explorer"),
                                new Document("case", new Document("$and", Arrays.asList(
                                        new Document("$gt", Arrays.asList("$repeat_ratio", 0.5)),
                                        new Document("$gt", Arrays.asList("$avg_stars", 3))
                                ))).append("then", "comfort-seeker"),
                                new Document("case",
                                        new Document("$lt", Arrays.asList("$avg_stars", 2.5))
                                ).append("then", "budget-hunter")
                        )).append("default", "upgrader")))
                ),

                new Document("$merge", new Document("into", "travellers")
                        .append("on", "_id")
                        .append("whenMatched", Arrays.asList(
                                new Document("$set", new Document("user_segment", "$$new.user_segment"))
                        ))
                        .append("whenNotMatched", "discard")
                )
        );

        travellers.aggregate(pipeline).toCollection();
        System.out.println("[Init] Step 3 completato.");
    }

    // ─────────────────────────────────────────────────────────────
    // STEP 4 — segment_distribution + preference_distribution
    // Usa user_segment (Step 3) e trip_budget/season già nei past_trips.
    // Il $merge aggiunge solo questi due campi senza toccare
    // totalVisits e seasonality scritti negli step precedenti.
    // ─────────────────────────────────────────────────────────────
    private void populateSegmentAndPreferenceDistribution() {
        System.out.println("[Init] Step 4 — segment_distribution e preference_distribution...");

        MongoCollection<Document> travellers = mongoTemplate.getCollection("travellers");

        List<Document> pipeline = Arrays.asList(

                new Document("$unwind", "$past_trips"),

                // Raggruppa per hotel: conta visite totali e breakdown per segmento/budget/stagione
                new Document("$group", new Document("_id", "$past_trips.hotel_name")
                        .append("total", new Document("$sum", 1))

                        // segment counts
                        .append("explorer",       new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$user_segment", "explorer")), 1, 0))))
                        .append("comfort_seeker", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$user_segment", "comfort-seeker")), 1, 0))))
                        .append("upgrader",       new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$user_segment", "upgrader")), 1, 0))))
                        .append("budget_hunter",  new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$user_segment", "budget-hunter")), 1, 0))))

                        // budget counts
                        .append("budget_low",    new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.trip_budget", "low")), 1, 0))))
                        .append("budget_medium", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.trip_budget", "medium")), 1, 0))))
                        .append("budget_high",   new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.trip_budget", "high")), 1, 0))))

                        // season counts
                        .append("season_spring", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.season", "spring")), 1, 0))))
                        .append("season_summer", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.season", "summer")), 1, 0))))
                        .append("season_autumn", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.season", "autumn")), 1, 0))))
                        .append("season_winter", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.season", "winter")), 1, 0))))
                ),

                // Calcola proporzioni e dominant per ogni distribuzione
                new Document("$project", new Document("_id", 0)
                        .append("HotelName", "$_id")
                        .append("guestStats", new Document()

                                .append("segment_distribution", new Document()
                                        .append("explorer",       new Document("$divide", Arrays.asList("$explorer",       "$total")))
                                        .append("comfort-seeker", new Document("$divide", Arrays.asList("$comfort_seeker", "$total")))
                                        .append("upgrader",       new Document("$divide", Arrays.asList("$upgrader",       "$total")))
                                        .append("budget-hunter",  new Document("$divide", Arrays.asList("$budget_hunter",  "$total")))
                                        .append("dominant_segment", new Document("$switch", new Document("branches", Arrays.asList(
                                                new Document("case", new Document("$and", Arrays.asList(
                                                        new Document("$gte", Arrays.asList("$explorer", "$comfort_seeker")),
                                                        new Document("$gte", Arrays.asList("$explorer", "$upgrader")),
                                                        new Document("$gte", Arrays.asList("$explorer", "$budget_hunter"))
                                                ))).append("then", "explorer"),
                                                new Document("case", new Document("$and", Arrays.asList(
                                                        new Document("$gte", Arrays.asList("$comfort_seeker", "$explorer")),
                                                        new Document("$gte", Arrays.asList("$comfort_seeker", "$upgrader")),
                                                        new Document("$gte", Arrays.asList("$comfort_seeker", "$budget_hunter"))
                                                ))).append("then", "comfort-seeker"),
                                                new Document("case", new Document("$and", Arrays.asList(
                                                        new Document("$gte", Arrays.asList("$upgrader", "$explorer")),
                                                        new Document("$gte", Arrays.asList("$upgrader", "$comfort_seeker")),
                                                        new Document("$gte", Arrays.asList("$upgrader", "$budget_hunter"))
                                                ))).append("then", "upgrader")
                                        )).append("default", "budget-hunter")))
                                )

                                .append("preference_distribution", new Document()
                                        .append("budget", new Document()
                                                .append("low",    new Document("$divide", Arrays.asList("$budget_low",    "$total")))
                                                .append("medium", new Document("$divide", Arrays.asList("$budget_medium", "$total")))
                                                .append("high",   new Document("$divide", Arrays.asList("$budget_high",   "$total")))
                                                .append("dominant", new Document("$switch", new Document("branches", Arrays.asList(
                                                        new Document("case", new Document("$and", Arrays.asList(
                                                                new Document("$gte", Arrays.asList("$budget_low", "$budget_medium")),
                                                                new Document("$gte", Arrays.asList("$budget_low", "$budget_high"))
                                                        ))).append("then", "low"),
                                                        new Document("case", new Document("$and", Arrays.asList(
                                                                new Document("$gte", Arrays.asList("$budget_medium", "$budget_low")),
                                                                new Document("$gte", Arrays.asList("$budget_medium", "$budget_high"))
                                                        ))).append("then", "medium")
                                                )).append("default", "high")))
                                        )
                                        .append("preferred_season", new Document()
                                                .append("spring", new Document("$divide", Arrays.asList("$season_spring", "$total")))
                                                .append("summer", new Document("$divide", Arrays.asList("$season_summer", "$total")))
                                                .append("autumn", new Document("$divide", Arrays.asList("$season_autumn", "$total")))
                                                .append("winter", new Document("$divide", Arrays.asList("$season_winter", "$total")))
                                                .append("dominant", new Document("$switch", new Document("branches", Arrays.asList(
                                                        new Document("case", new Document("$and", Arrays.asList(
                                                                new Document("$gte", Arrays.asList("$season_spring", "$season_summer")),
                                                                new Document("$gte", Arrays.asList("$season_spring", "$season_autumn")),
                                                                new Document("$gte", Arrays.asList("$season_spring", "$season_winter"))
                                                        ))).append("then", "spring"),
                                                        new Document("case", new Document("$and", Arrays.asList(
                                                                new Document("$gte", Arrays.asList("$season_summer", "$season_spring")),
                                                                new Document("$gte", Arrays.asList("$season_summer", "$season_autumn")),
                                                                new Document("$gte", Arrays.asList("$season_summer", "$season_winter"))
                                                        ))).append("then", "summer"),
                                                        new Document("case", new Document("$and", Arrays.asList(
                                                                new Document("$gte", Arrays.asList("$season_autumn", "$season_spring")),
                                                                new Document("$gte", Arrays.asList("$season_autumn", "$season_summer")),
                                                                new Document("$gte", Arrays.asList("$season_autumn", "$season_winter"))
                                                        ))).append("then", "autumn")
                                                )).append("default", "winter")))
                                        )
                                )
                        )
                ),

                // Merge: aggiunge solo segment_distribution e preference_distribution
                // senza sovrascrivere totalVisits e seasonality degli step precedenti
                new Document("$merge", new Document("into", "hotels")
                        .append("on", "HotelName")
                        .append("whenMatched", Arrays.asList(
                                new Document("$set", new Document()
                                        .append("guestStats.segment_distribution",    "$$new.guestStats.segment_distribution")
                                        .append("guestStats.preference_distribution", "$$new.guestStats.preference_distribution")
                                )
                        ))
                        .append("whenNotMatched", "discard")
                )
        );

        travellers.aggregate(pipeline).toCollection();
        System.out.println("[Init] Step 4 completato.");
    }
    // Step 5: calcola travelType su tutti i nodi Traveller in Neo4j
    private void populateTravelTypes() {
        System.out.println("[Init] Step 5 — travelType sui nodi Traveller Neo4j...");
        travellerNodeRepository.computeAndStoreTravelTypeAll();
        System.out.println("[Init] Step 5 completato.");
    }
}