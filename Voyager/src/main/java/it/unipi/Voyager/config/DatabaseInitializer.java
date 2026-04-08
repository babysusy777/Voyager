package it.unipi.Voyager.config;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Order(2) //Eseguito dopo DataIngestionService
public class DatabaseInitializer {

    @Autowired
    private MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeHotelStats() {
        System.out.println("[Init] Avvio popolamento guestStats sugli hotel...");

        MongoCollection<Document> travellers = mongoTemplate.getCollection("travellers");

        // Pipeline: dai past_trips dei travellers, aggrega per hotel_name
        // e calcola totalVisits e counts per stagione, poi fa writeback su hotels
        List<Document> pipeline = Arrays.asList(

                // 1. Decomponila l'array past_trips
                new Document("$unwind", "$past_trips"),

                // 2. Decomponila l'array hotel_name dentro il trip
                new Document("$unwind", "$past_trips.hotel_name"),

                // 3. Raggruppa per hotel_name: conta visite totali e per stagione
                new Document("$group", new Document("_id", "$past_trips.hotel_name.hotel_name")
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

                // 4. Costruisce il documento guestStats da scrivere
                new Document("$project", new Document("_id", 0)
                        .append("hotelName", "$_id")
                        .append("guestStats", new Document("totalVisits", "$totalVisits")
                                .append("seasonality", new Document("counts", new Document()
                                        .append("spring", "$spring")
                                        .append("summer", "$summer")
                                        .append("autumn", "$autumn")
                                        .append("winter", "$winter")
                                ))
                        )
                ),

                // 5. Merge su hotels matchando per HotelName
                new Document("$merge", new Document("into", "hotels")
                        .append("on", "HotelName")  // field usato come join key
                        .append("whenMatched", Arrays.asList(
                                new Document("$set", new Document("guestStats", "$$new.guestStats"))
                        ))
                        .append("whenNotMatched", "discard")  // ignora hotel non trovati
                )
        );

        travellers.aggregate(pipeline).toCollection();

        System.out.println("[Init] guestStats popolato su tutti gli hotel.");
    }
}