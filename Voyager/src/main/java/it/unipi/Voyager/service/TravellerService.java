package it.unipi.Voyager.service;

import it.unipi.Voyager.dto.TravelHabitDTO;
import it.unipi.Voyager.dto.TravellerSegmentDTO;
import it.unipi.Voyager.dto.TripDTO;
import it.unipi.Voyager.dto.TripFrequencyDTO;
import it.unipi.Voyager.model.Traveller;
import it.unipi.Voyager.repository.TravellerRepository;
import it.unipi.Voyager.repository.graph.TravellerGraphRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import com.mongodb.client.result.UpdateResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TravellerService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TravellerRepository travellerRepository;

    @Autowired
    private HotelService hotelService;

    @Autowired
    private TravellerGraphRepository travellerNodeRepository;


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
        // 1. Recupero l'utente tramite email [cite: 89, 255]
        Traveller traveller = travellerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Traveller non trovato con email: " + email));

        // 2. Validazione: servono almeno 2 viaggi per calcolare un intervallo (gap)
        if (traveller.getTrips() == null || traveller.getTrips().size() < 2) {
            throw new RuntimeException("Dati insufficienti: sono necessari almeno 2 viaggi per l'analisi della frequenza.");
        }

        // 3. Esecuzione della query di aggregazione sull'ID dell'utente trovato [cite: 291]
        return travellerRepository.getTripFrequency(traveller.getId());
    }

    public void upsertTrip(String email, TripDTO tripDto) {
        Document tripDoc = new Document();
        mongoTemplate.getConverter().write(tripDto, tripDoc);

        Traveller traveller = travellerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Traveller non trovato con email: " + email));

        // Usa email come chiave di ricerca
        Document query = new Document("email", email)
                .append("past_trips.trip_name", tripDto.getTripName());

        Document updateFields = new Document("past_trips.$.city", tripDto.getCity())
                .append("past_trips.$.hotel_name", tripDoc.get("hotels"))
                .append("past_trips.$.season", tripDto.getSeason())
                .append("past_trips.$.date", tripDto.getDate())
                .append("past_trips.$.rating_given", tripDto.getRatingGiven());

        UpdateResult result = mongoTemplate.getCollection("travellers")
                .updateOne(query, new Document("$set", updateFields));

        if (result.getMatchedCount() == 0) {
            // Trip non esiste → push
            mongoTemplate.getCollection("travellers").updateOne(
                    new Document("email", email),
                    new Document("$push", new Document("past_trips", tripDoc))
            );
        }

        // Writeback hotel stats
        if (tripDto.getHotels() != null) {
            for (Traveller.Trip.HotelSummary hotel : tripDto.getHotels()) {
                hotelService.updateHotelStatsOnNewTrip(
                        hotel.getHotelName(),
                        tripDto.getSeason(),
                        traveller.getUserSegment(),  // recuperato prima con findByEmail
                        tripDto.getTripBudget()      // se presente nel DTO
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
        // Ricalcola travelType sul nodo Neo4j dopo un nuovo trip
        travellerNodeRepository.computeAndStoreTravelType(email);
    }

    public List<Traveller.Trip> getTripsSortedByDate(String userId) {
        List<Document> pipeline = Arrays.asList(
                new Document("$match", new Document("userId", userId)),
                new Document("$unwind", "$trips"),
                new Document("$sort", new Document("trips.date", -1)),
                new Document("$replaceRoot", new Document("newRoot", "$trips"))
        );

        List<Document> rawResults = new ArrayList<>();
        mongoTemplate.getCollection("travellers").aggregate(pipeline).into(rawResults);


        return rawResults.stream()
                .map(doc -> mongoTemplate.getConverter().read(Traveller.Trip.class, doc))
                .collect(Collectors.toList());
    }

    public String getTravelerStarTrend(String userId) {
        List<Document> pipeline = Arrays.asList(
                // Match stage
                new Document("$match", new Document("userId", userId)),

                // Unwind stages
                new Document("$unwind", "$past_trips"),
                new Document("$unwind", "$past_trips.hotel_name"),

                // Project stage con lo switch (Plain Mongo style)
                new Document("$project", new Document("tripDate", "$past_trips.date")
                        .append("starValue", new Document("$switch", new Document("branches", Arrays.asList(
                                new Document("case", new Document("$eq", Arrays.asList("$past_trips.hotel_name.stars", "oneStar"))).append("then", 1),
                                new Document("case", new Document("$eq", Arrays.asList("$past_trips.hotel_name.stars", "twoStars"))).append("then", 2),
                                new Document("case", new Document("$eq", Arrays.asList("$past_trips.hotel_name.stars", "threeStars"))).append("then", 3),
                                new Document("case", new Document("$eq", Arrays.asList("$past_trips.hotel_name.stars", "fourStars"))).append("then", 4),
                                new Document("case", new Document("$eq", Arrays.asList("$past_trips.hotel_name.stars", "fiveStars"))).append("then", 5)
                        )).append("default", 0)))),

                // Sort e Group
                new Document("$sort", new Document("tripDate", 1)),
                new Document("$group", new Document("_id", "$_id")
                        .append("starHistory", new Document("$push", "$starValue")))
        );

        Document res = mongoTemplate.getCollection("travellers").aggregate(pipeline).first();

        return (res != null) ? analyzeTrend(res.getList("starHistory", Integer.class)) : "DATI INSUFFICIENTI";
    }

    private String analyzeTrend(List<Integer> stars) {

        if (stars == null || stars.isEmpty()) {
            return "DATI INSUFFICIENTI";
        }
        if (stars.size() < 2) {
            return "STABILE (Dato singolo)";
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
            return "CRESCENTE (L'utente sta scegliendo hotel di qualità sempre maggiore)";
        } else if (diff < -threshold) {
            return "DECRESCENTE (L'utente sta riducendo lo standard degli hotel)";
        } else {
            return "STABILE (Le preferenze di qualità rimangono costanti nel tempo)";
        }
    }

    public TravellerSegmentDTO getTravellerSegment(String email) {
        Traveller traveller = travellerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Traveller non trovato"));

        TravellerSegmentDTO dto = new TravellerSegmentDTO();
        dto.setSegment(traveller.getUserSegment());
        return dto;
    }
}