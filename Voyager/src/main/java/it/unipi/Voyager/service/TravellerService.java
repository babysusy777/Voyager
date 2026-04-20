package it.unipi.Voyager.service;

import it.unipi.Voyager.dto.*;
import it.unipi.Voyager.model.Traveller;
import it.unipi.Voyager.repository.TravellerRepository;
import it.unipi.Voyager.repository.graph.TravellerGraphRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import com.mongodb.client.result.UpdateResult;
import org.springframework.stereotype.Service;
import it.unipi.Voyager.config.Neo4jSyncService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TravellerService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private Neo4jSyncService neo4jSyncService;

    @Autowired
    private TravellerRepository travellerRepository;

    @Autowired
    private HotelService hotelService;

    @Autowired
    private TravellerGraphRepository travellerNodeRepository;

    public Traveller setPreferences(TravellerConfigRequest request) {
        Traveller traveller = travellerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        traveller.setGender(request.getGender());
        traveller.setAge(request.getAge());
        traveller.setCountry(request.getCountry());

        Traveller.Preferences preferences = new Traveller.Preferences();
        preferences.setBudget(request.getBudget());
        preferences.setSeason(request.getSeason());
        if (request.getTravelType() != null) {
            // .name() trasforma l'Enum (es. ADVENTURE) in stringa "ADVENTURE"
            preferences.setTravelType(request.getTravelType().name());
        }
        traveller.setPreferences(preferences);

        Traveller saved = travellerRepository.save(traveller);
        neo4jSyncService.syncTravellerByEmail(request.getEmail());
        return saved;
    }

     // Se il viaggio con lo stesso nome esiste, lo aggiorna.
     // Se non esiste, lo aggiunge alla lista past_trips.
     public TravelHabitDTO getTravelHabitsByEmail(String email) {
         // 1. Cerchiamo il traveller tramite l'email
         Traveller traveller = travellerRepository.findByEmail(email)
                 .orElseThrow(() -> new RuntimeException("Traveller non trovato con email: " + email));


         // 2. Chiamiamo la query di aggregazione usando l'ID dell'utente trovato
         return travellerRepository.getTravelHabits(traveller.getId());
     }

    public TripFrequencyDTO getTripFrequencyByEmail(String email) {
        // 1. Recupero l'utente tramite email

        Traveller traveller = travellerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Traveller non trovato con email: " + email));

        // 2. Validazione: servono almeno 2 viaggi per calcolare un intervallo (gap)
        if (traveller.getTrips() == null || traveller.getTrips().size() < 2) {
            throw new RuntimeException("Dati insufficienti: sono necessari almeno 2 viaggi per l'analisi della frequenza.");
        }

        // 3. Esecuzione della query di aggregazione sull'ID dell'utente trovato [cite: 291]
        return travellerRepository.getTripFrequency(traveller.getEmail());
    }

    public void deleteTrip(String email, TripDTO tripDTO) {
        Traveller traveller = travellerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Traveller non trovato con email: " + email));

    }

    public void updateTripPartial(String email, String tripName, Map<String, Object> updates) {
        // 1. Costruiamo il filtro per trovare il traveller e lo specifico viaggio nell'array
        Document query = new Document("email", email)
                .append("past_trips.trip_name", tripName);

        // 2. Prepariamo i campi di update
        Document setFields = new Document();

        updates.forEach((key, value) -> {
            // Mappiamo i campi del DTO/Mappa ai nomi reali sul DB (es: budget, rating_given)
            // Usiamo la sintassi past_trips.$.nomeCampo
            String mongoKey = "past_trips.$." + convertToDbFieldName(key);

            // Gestione speciale per la lista di hotel se viene passata come lista di mappe
            if (key.equals("hotels") && value instanceof List) {
                setFields.append(mongoKey, value);
            } else {
                setFields.append(mongoKey, value);
            }
        });

        if (setFields.isEmpty()) return;

        // 3. Eseguiamo l'update
        UpdateResult result = mongoTemplate.getCollection("travellers")
                .updateOne(query, new Document("$set", setFields));

        if (result.getMatchedCount() > 0) {
            // 4. Se l'update ha toccato campi sensibili, ricalcoliamo segmenti e sync
            // Nota: Qui potresti voler aggiungere un controllo se 'updates' contiene budget o rating
            TravellerSegmentDTO newSegment = travellerRepository.computeSegment(email);
            if (newSegment != null) {
                mongoTemplate.getCollection("travellers").updateOne(
                        new Document("email", email),
                        new Document("$set", new Document("user_segment", newSegment.getSegment()))
                );
            }
            travellerNodeRepository.computeAndStoreTravelType(email);
            neo4jSyncService.syncTravellerByEmail(email);
        } else {
            throw new RuntimeException("Viaggio o Traveller non trovato.");
        }
    }

    // Metodo helper per mappare i nomi dei campi Java/JSON a quelli definiti con @Field in MongoDB
    private String convertToDbFieldName(String jsonKey) {
        switch (jsonKey) {
            case "tripName": return "trip_name";
            case "ratingGiven": return "rating_given";
            default: return jsonKey; // city, budget, season, date, hotels coincidono
        }
    }

    public void upsertTrip(String email, TripDTO tripDto) {
        //Document tripDoc = new Document();
        //mongoTemplate.getConverter().write(tripDto, tripDoc);

        Traveller traveller = travellerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Traveller non trovato con email: " + email));

        List<Document> hotelDocs = new ArrayList<>();
        if (tripDto.getHotels() != null) {
            for (Traveller.Trip.HotelSummary h : tripDto.getHotels()) {
                hotelDocs.add(new Document("hotelName", h.getHotelName())
                        .append("hotelStars", h.getHotelStars()));
            }
        }

        // Usa email come chiave di ricerca
        Document query = new Document("email", email)
                .append("past_trips.trip_name", tripDto.getTripName());

        Document updateFields = new Document("past_trips.$.city", tripDto.getCity())
                .append("past_trips.$.hotels", hotelDocs)
                .append("past_trips.$.season", tripDto.getSeason())
                .append("past_trips.$.date", tripDto.getDate())
                .append("past_trips.$.budget", tripDto.getBudget())
                .append("past_trips.$.rating_given", tripDto.getRatingGiven());

        UpdateResult result = mongoTemplate.getCollection("travellers")
                .updateOne(query, new Document("$set", updateFields));

        if (result.getMatchedCount() == 0) {
            // Trip non esiste → push
            Document newTrip = new Document("trip_name", tripDto.getTripName())
                .append("city", tripDto.getCity())
                .append("hotels", hotelDocs)
                .append("season", tripDto.getSeason())
                .append("date", tripDto.getDate())
                .append("rating_given", tripDto.getRatingGiven()).append("budget", tripDto.getBudget());



            mongoTemplate.getCollection("travellers").updateOne(
                    new Document("email", email),
                    new Document("$push", new Document("past_trips", newTrip))
            );
        }

        // Writeback hotel stats
        if (tripDto.getHotels() != null) {
            for (Traveller.Trip.HotelSummary hotel : tripDto.getHotels()) {
                hotelService.updateHotelStatsOnNewTrip(
                        hotel.getHotelName(),
                        tripDto.getCity(),            // ← aggiunto
                        tripDto.getSeason(),
                        traveller.getUserSegment(),
                        tripDto.getBudget()
                );
            }
        }

        // Ricalcola e aggiorna user_segment del traveller
        TravellerSegmentDTO newSegment = travellerRepository.computeSegment(email);
        if (newSegment != null) {
            mongoTemplate.getCollection("travellers").updateOne(
                    new Document("email", email),
                    new Document("$set", new Document("user_segment", newSegment.getSegment()))
            );
        }

        // Sync Neo4j dopo update
        // Ricalcola travelType sul nodo Neo4j dopo un nuovo trip
        neo4jSyncService.syncTravellerByEmail(email);
        travellerNodeRepository.computeAndStoreTravelType(email);
    }

    public List<Traveller.Trip> getTripsSortedByDate(String email) {
        List<Document> pipeline = Arrays.asList(
                new Document("$match", new Document("email", email)),
                new Document("$unwind", "$past_trips"),
                new Document("$sort", new Document("past_trips.date", -1)),
                new Document("$replaceRoot", new Document("newRoot", "$past_trips"))
        );

        List<Document> rawResults = new ArrayList<>();
        mongoTemplate.getCollection("travellers").aggregate(pipeline).into(rawResults);

        return rawResults.stream()
                .map(doc -> mongoTemplate.getConverter().read(Traveller.Trip.class, doc))
                .collect(Collectors.toList());
    }

    public String getTravelerStarTrend(String email) {
        List<Document> pipeline = Arrays.asList(
                new Document("$match", new Document("email", email)),
                new Document("$unwind", "$past_trips"),
                // Prendiamo il primo hotel di ogni viaggio (assumendo ce ne sia uno principale)
                // e convertiamo la stringa "threeStar" in numero
                new Document("$project", new Document("tripDate", "$past_trips.date")
                        .append("starValue", new Document("$switch", new Document("branches", Arrays.asList(
                                // Gestione formato "oneStar" O "1"
                                new Document("case", new Document("$in", Arrays.asList(new Document("$arrayElemAt", Arrays.asList("$past_trips.hotels.hotelStars", 0)), Arrays.asList("oneStar", "1")))).append("then", 1),
                                // Gestione formato "twoStar" O "2"
                                new Document("case", new Document("$in", Arrays.asList(new Document("$arrayElemAt", Arrays.asList("$past_trips.hotels.hotelStars", 0)), Arrays.asList("twoStar", "2")))).append("then", 2),
                                // Gestione formato "threeStar" O "3"
                                new Document("case", new Document("$in", Arrays.asList(new Document("$arrayElemAt", Arrays.asList("$past_trips.hotels.hotelStars", 0)), Arrays.asList("threeStar", "3")))).append("then", 3),
                                // Gestione formato "fourStar" O "4"
                                new Document("case", new Document("$in", Arrays.asList(new Document("$arrayElemAt", Arrays.asList("$past_trips.hotels.hotelStars", 0)), Arrays.asList("fourStar", "4")))).append("then", 4),
                                // Gestione formato "fiveStar" O "5"
                                new Document("case", new Document("$in", Arrays.asList(new Document("$arrayElemAt", Arrays.asList("$past_trips.hotels.hotelStars", 0)), Arrays.asList("fiveStar", "5")))).append("then", 5)
                        )).append("default", 0)))),

                new Document("$match", new Document("starValue", new Document("$gt", 0))), // Escludi i viaggi senza hotel
                new Document("$sort", new Document("tripDate", 1)),
                new Document("$group", new Document("_id", "$_id")
                        .append("starHistory", new Document("$push", "$starValue")))
        );

        Document res = mongoTemplate.getCollection("travellers").aggregate(pipeline).first();

        if (res == null || res.getList("starHistory", Integer.class) == null) {
            return "INSUFFICIENT DATA";
        }

        return analyzeTrend(res.getList("starHistory", Integer.class));
    }

    private String analyzeTrend(List<Integer> stars) {

        if (stars == null || stars.isEmpty()) {
            return "INSUFFICIENT DATA";
        }
        if (stars.size() < 2) {
            return "STABLE (Single data point)";
        }

        int mid = stars.size() / 2;

        double firstHalfAvg = stars.subList(0, mid).stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        double secondHalfAvg = stars.subList(mid, stars.size()).stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        double diff = secondHalfAvg - firstHalfAvg;
        double threshold = 0.2;

        if (diff > threshold) {
            return "INCREASING (The user is choosing hotels of progressively higher quality)";
        } else if (diff < -threshold) {
            return "DECREASING (The user is reducing their hotel quality standards)";
        } else {
            return "STABLE (Quality preferences remain constant over time)";
        }
    }

    public TravellerSegmentDTO getTravellerSegment(String email) {
        Traveller traveller = travellerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Traveller not found"));

        TravellerSegmentDTO dto = new TravellerSegmentDTO();
        dto.setSegment(traveller.getUserSegment());
        return dto;
    }
}