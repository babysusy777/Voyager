package it.unipi.Voyager.config;

import com.mongodb.client.MongoCollection;
import it.unipi.Voyager.repository.graph.TravellerGraphRepository;
import it.unipi.Voyager.service.AttractionService;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
    private Neo4jSyncService neo4jSyncService;

    @Autowired
    private TravellerGraphRepository travellerNodeRepository;

    @Autowired
    private AttractionService attractionService;

    public void initializeHotelStats() {
        // Esegui gli step solo se guestStats non è ancora stato popolato
        // Controlla se almeno un hotel ha totalVisits > 0
        long alreadyPopulated = mongoTemplate.getCollection("hotels")
                .countDocuments(new Document("guestStats.totalVisits", new Document("$gt", 0)));

        if (alreadyPopulated > 0) {
            System.out.println("[Init] guestStats già popolato, skip.");
            return;
        }

        // Step 1: totalVisits + seasonality.counts su ogni hotel
        populateGuestStats();
        updateCityIndexes();
        // Step 2: city_category_avg_visits (dipende da Step 1)
        populateCityCategoryAvgVisits();
        // Step 3: calcola user_segment e lo salva su ogni traveller
        populateTravellerSegments();
        // Step 4: segment_distribution + preference_distribution su ogni hotel (dipende da Step 3)
        populateSegmentAndPreferenceDistribution();
        populateCitySeasonality();
        // Step 5: calcola travelType su tutti i nodi Traveller in Neo4j
        populateTravelTypes();
    }

    private void updateCityIndexes() {
        System.out.println("[Init] Sincronizzazione totalVisits nelle città...");

        // 1. Prendi tutti i nomi delle città che hai nel DB degli hotel
        List<String> cities = mongoTemplate.getCollection("hotels")
                .distinct("cityName", String.class)
                .into(new ArrayList<>());

        for (String cityName : cities) {
            // 2. Chiedi a MongoDB: "Somma tutti i totalVisits degli hotel di questa città"
            Aggregation agg = Aggregation.newAggregation(
                    Aggregation.match(Criteria.where("cityName").is(cityName)),
                    Aggregation.group("cityName")
                            .count().as("hotelCount")
                            .sum("guestStats.totalVisits").as("totalVisits")
            );

            AggregationResults<Document> results = mongoTemplate.aggregate(agg, "hotels", Document.class);
            Document res = results.getUniqueMappedResult();

            if (res != null) {
                int total = res.getInteger("totalVisits", 0);
                int count = res.getInteger("hotelCount", 0);
                double ratio = (count > 0) ? (double) total / count : 0.0;

                // 3. Vai nella collezione "cities" e scrivi i valori reali
                Query query = new Query(Criteria.where("cityName").is(cityName));
                Update update = new Update()
                        .set("city_index.total_visits", total)
                        .set("city_index.hotel_count", count)
                        .set("city_index.demand_ratio", ratio);

                mongoTemplate.updateFirst(query, update, "cities");
            }
        }
        System.out.println("[Init] City Index aggiornato con successo.");
    }

    private void populateGuestStats() {
        System.out.println("[Init] Step 1 — totalVisits e seasonality.counts...");

        MongoCollection<Document> travellers = mongoTemplate.getCollection("travellers");

        List<Document> pipeline = Arrays.asList(
                // 1. Srotola i viaggi
                new Document("$unwind", "$past_trips"),

                // 2. Srotola la lista degli hotel (essendo ora un array hotels: [ { hotelName: ... } ])
                new Document("$unwind", "$past_trips.hotels"),

                // 3. Srotola la lista delle città (essendo ora city: [ 'Rome' ])
                new Document("$unwind", "$past_trips.city"),

                // Raggruppa per coppia (hotelName, cityName)
                new Document("$group", new Document("_id", new Document()
                        .append("hotelName", "$past_trips.hotels.hotelName") // Path aggiornato
                        .append("cityName", "$past_trips.city"))           // Path dopo unwind
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
                        .append("HotelName", "$_id.hotelName")
                        .append("cityName", "$_id.cityName")
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
                        .append("on", Arrays.asList("HotelName", "cityName"))
                        .append("whenMatched", Arrays.asList(
                                new Document("$set", new Document("guestStats", "$$new.guestStats"))
                        ))
                        .append("whenNotMatched", "discard")
                )
        );

        travellers.aggregate(pipeline).toCollection();
        System.out.println("[Init] Step 1 completato.");
    }

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

    private void populateTravellerSegments() {
        System.out.println("[Init] Step 3 — user_segment sui travellers...");

        MongoCollection<Document> travellers = mongoTemplate.getCollection("travellers");

        List<Document> pipeline = Arrays.asList(
                // 1. Srotoliamo i viaggi
                new Document("$unwind", "$past_trips"),

                // 2. Srotoliamo le città (perché past_trips.city è un array)
                new Document("$unwind", "$past_trips.city"),

                // 3. Srotoliamo gli hotel (perché past_trips.hotels è un array)
                new Document("$unwind", "$past_trips.hotels"),
                // --- NUOVO STEP: Conversione Stringa -> Numero ---
                new Document("$addFields", new Document("numericStars",
                        new Document("$switch", new Document("branches", Arrays.asList(
                                new Document("case", new Document("$in", Arrays.asList("$past_trips.hotels.hotelStars", Arrays.asList("oneStar", "1")))).append("then", 1),
                                new Document("case", new Document("$in", Arrays.asList("$past_trips.hotels.hotelStars", Arrays.asList("twoStar", "2")))).append("then", 2),
                                new Document("case", new Document("$in", Arrays.asList("$past_trips.hotels.hotelStars", Arrays.asList("threeStar", "3")))).append("then", 3),
                                new Document("case", new Document("$in", Arrays.asList("$past_trips.hotels.hotelStars", Arrays.asList("fourStar", "4")))).append("then", 4),
                                new Document("case", new Document("$in", Arrays.asList("$past_trips.hotels.hotelStars", Arrays.asList("fiveStar", "5")))).append("then", 5)
                        )).append("default", 0))
                )),

                new Document("$group", new Document("_id", "$_id")
                        // Raccogliamo le città uniche (ora sono stringhe singole dopo l'unwind)
                        .append("unique_cities", new Document("$addToSet", "$past_trips.city"))
                        // Media delle stelle accedendo al nuovo path hotels.hotelStars
                        // Nota: se hotelStars è salvato come Stringa, usa {$toDouble: "$past_trips.hotels.hotelStars"}
                        .append("avg_stars", new Document("$avg", "$past_trips.hotels.hotelStars"))
                        .append("total_trips", new Document("$sum", 1))
                ),

                new Document("$project", new Document()
                        .append("unique_cities_count", new Document("$size", "$unique_cities"))
                        .append("avg_stars", 1)
                        .append("total_trips", 1)
                        .append("repeat_ratio", new Document("$subtract", Arrays.asList(
                                1,
                                new Document("$divide", Arrays.asList(
                                        new Document("$size", "$unique_cities"),
                                        "$total_trips"
                                ))
                        )))
                ),

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

    private void populateSegmentAndPreferenceDistribution() {
        System.out.println("[Init] Step 4 — segment_distribution e preference_distribution...");

        MongoCollection<Document> travellers = mongoTemplate.getCollection("travellers");

        List<Document> pipeline = Arrays.asList(
                // 1. Srotola i viaggi
                new Document("$unwind", "$past_trips"),

                // 2. Srotola le città (ora array)
                new Document("$unwind", "$past_trips.city"),

                // 3. Srotola gli hotel (ora array di oggetti)
                new Document("$unwind", "$past_trips.hotels"),

                // Raggruppa per coppia (hotelName, cityName)
                new Document("$group", new Document("_id", new Document()
                        .append("hotelName", "$past_trips.hotels.hotelName") // Path aggiornato
                        .append("cityName",  "$past_trips.city"))           // Path aggiornato
                        .append("total", new Document("$sum", 1))

                        // segment counts (il campo user_segment è sulla radice del traveller)
                        .append("explorer",       new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$user_segment", "explorer")), 1, 0))))
                        .append("comfort_seeker", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$user_segment", "comfort-seeker")), 1, 0))))
                        .append("upgrader",       new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$user_segment", "upgrader")), 1, 0))))
                        .append("budget_hunter",  new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$user_segment", "budget-hunter")), 1, 0))))

                        // budget counts (il campo è stato rinominato in 'budget')
                        .append("budget_low",    new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.budget", "low")), 1, 0))))
                        .append("budget_medium", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.budget", "medium")), 1, 0))))
                        .append("budget_high",   new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.budget", "high")), 1, 0))))

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

                new Document("$project", new Document("_id", 0)
                        .append("HotelName", "$_id.hotelName")
                        .append("cityName",  "$_id.cityName")
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

                new Document("$merge", new Document("into", "hotels")
                        .append("on", Arrays.asList("HotelName", "cityName"))
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

    private void populateTravelTypes() {
        System.out.println("[Init] Step 5 — travelType sui nodi Traveller Neo4j...");
        neo4jSyncService.syncAll();
        travellerNodeRepository.computeAndStoreTravelTypeAll();
        System.out.println("[Init] Step 5 completato.");
    }

    private void populateCitySeasonality() {
        System.out.println("[Init] Step 5b — seasonality cities...");

        MongoCollection<Document> travellers = mongoTemplate.getCollection("travellers");

        List<Document> pipeline = Arrays.asList(
                // 1. Srotola i viaggi
                new Document("$unwind", "$past_trips"),

                // 2. Srotola l'array delle città (fondamentale perché city: [ 'Rome' ])
                new Document("$unwind", "$past_trips.city"),

                // Raggruppa per il nome della città singola
                new Document("$group", new Document("_id", "$past_trips.city")
                        .append("spring", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.season", "spring")), 1, 0))))
                        .append("summer", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.season", "summer")), 1, 0))))
                        .append("autumn", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.season", "autumn")), 1, 0))))
                        .append("winter", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.season", "winter")), 1, 0))))
                        .append("total", new Document("$sum", 1))
                ),

                // Calcola peak_season e concentration_ratio
                new Document("$project", new Document("_id", 1)
                        .append("spring", 1).append("summer", 1)
                        .append("autumn", 1).append("winter", 1)
                        .append("peak_season", new Document("$switch", new Document("branches", Arrays.asList(
                                new Document("case", new Document("$and", Arrays.asList(
                                        new Document("$gte", Arrays.asList("$spring", "$summer")),
                                        new Document("$gte", Arrays.asList("$spring", "$autumn")),
                                        new Document("$gte", Arrays.asList("$spring", "$winter"))
                                ))).append("then", "spring"),
                                new Document("case", new Document("$and", Arrays.asList(
                                        new Document("$gte", Arrays.asList("$summer", "$spring")),
                                        new Document("$gte", Arrays.asList("$summer", "$autumn")),
                                        new Document("$gte", Arrays.asList("$summer", "$winter"))
                                ))).append("then", "summer"),
                                new Document("case", new Document("$and", Arrays.asList(
                                        new Document("$gte", Arrays.asList("$autumn", "$spring")),
                                        new Document("$gte", Arrays.asList("$autumn", "$summer")),
                                        new Document("$gte", Arrays.asList("$autumn", "$winter"))
                                ))).append("then", "autumn")
                        )).append("default", "winter")))
                        .append("concentration_ratio", new Document("$divide", Arrays.asList(
                                new Document("$max", Arrays.asList("$spring", "$summer", "$autumn", "$winter")),
                                new Document("$cond", Arrays.asList(new Document("$eq", Arrays.asList("$total", 0)), 1, "$total"))
                        )))
                ),

                // Merge su cities usando cityName
                new Document("$project", new Document("cityName", "$_id")
                        .append("_id", 0)
                        .append("seasonality", new Document()
                                .append("spring", "$spring")
                                .append("summer", "$summer")
                                .append("autumn", "$autumn")
                                .append("winter", "$winter")
                                .append("peak_season", "$peak_season")
                                .append("concentration_ratio", "$concentration_ratio")
                        )
                        .append("new_total_visits", "$total")
                ),

                new Document("$merge", new Document("into", "cities")
                        .append("on", "cityName")
                        .append("whenMatched", Arrays.asList(
                                new Document("$set", new Document("seasonality", "$$new.seasonality"))

                        ))
                        .append("whenNotMatched", "discard")
                )
        );

        travellers.aggregate(pipeline).toCollection();
        System.out.println("[Init] Step 5b completato.");
    }

//    private void populateCityTopAttractions() {
//        System.out.println("[Init] Step 6 — Embedding Top Attractions into Cities...");
//
//        // 1. Prendi tutte le città da MongoDB
//        List<Document> cities = mongoTemplate.findAll(Document.class, "cities");
//
//        for (Document cityDoc : cities) {
//            String cityName = cityDoc.getString("cityName");
//            if (cityName == null) continue;
//
//            // 2. Recupera le top 5 attrazioni usando il Service (che usa Neo4j/Aggregations)
//            List<it.unipi.Voyager.dto.AttractionCentralityDTO> topAttractions =
//                    attractionService.getTopAttractions(cityName);
//
//            if (topAttractions != null && !topAttractions.isEmpty()) {
//                List<Document> attractionSummaries = new ArrayList<>();
//
//                for (it.unipi.Voyager.dto.AttractionCentralityDTO dto : topAttractions) {
//                    Document summary = new Document()
//                            .append("name", dto.getAttractionName())
//                            .append("type", dto.getCategory()) // Mappiamo 'category' del DTO su 'type' di Mongo
//                            .append("centrality_score", dto.getCentralityScore());
//                    attractionSummaries.add(summary);
//                }
//
//                // 3. Update atomico del documento City
//                mongoTemplate.updateFirst(
//                        org.springframework.data.mongodb.core.query.Query.query(
//                                org.springframework.data.mongodb.core.query.Criteria.where("cityName").is(cityName)),
//                        new org.springframework.data.mongodb.core.query.Update().set("top_attractions", attractionSummaries),
//                        "cities"
//                );
//            }
//        }
//        System.out.println("[Init] Step 6 completato.");
//    }
}