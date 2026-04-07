package it.unipi.Voyager.service;

import it.unipi.Voyager.dto.TripDTO;
import it.unipi.Voyager.model.Traveller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import com.mongodb.client.result.UpdateResult;
import org.springframework.stereotype.Service;

@Service
public class TravellerService {

    @Autowired
    private MongoTemplate mongoTemplate;

     // Se il viaggio con lo stesso nome esiste, lo aggiorna.
     // Se non esiste, lo aggiunge alla lista past_trips.

    public void upsertTrip(String userId, TripDTO tripDto) {

        Traveller.Trip trip = new Traveller.Trip();
        trip.setTripName(tripDto.getTripName());
        trip.setCity(tripDto.getCity());
        trip.setHotelName(tripDto.getHotelName());
        trip.setSeason(tripDto.getSeason());
        trip.setDate(tripDto.getDate());
        trip.setRatingGiven(tripDto.getRatingGiven());

        Query queryUpdate = new Query(
                Criteria.where("userId").is(userId).and("trips.trip_name").is(trip.getTripName())
        );

        Update updateExisting = new Update()
                .set("trips.$.city", trip.getCity())
                .set("trips.$.hotel_name", trip.getHotelName())
                .set("trips.$.season", trip.getSeason())
                .set("trips.$.date", trip.getDate())
                .set("trips.$.rating_given", trip.getRatingGiven());

        UpdateResult result = mongoTemplate.updateFirst(queryUpdate, updateExisting, Traveller.class);

        if (result.getMatchedCount() == 0) {
            Query queryPush = new Query(Criteria.where("userId").is(userId));
            Update updatePush = new Update().push("trips", trip);

            mongoTemplate.updateFirst(queryPush, updatePush, Traveller.class);
            System.out.println("Nuovo viaggio aggiunto con successo.");
        } else {
            System.out.println("Viaggio esistente aggiornato con successo.");
        }
    }
}