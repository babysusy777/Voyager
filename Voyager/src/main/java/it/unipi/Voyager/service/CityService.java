package it.unipi.Voyager.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;


import org.springframework.data.mongodb.core.aggregation.*;


import java.util.*;

@Service
public class CityService {
    @Autowired
    private MongoTemplate mongoTemplate;

    public void removeHotelFromCityMetrics(String cityName, String hotelId, String hotelName) {
        // 1. Cerchiamo la città per nome
        Query query = new Query(Criteria.where("cityName").is(cityName));
        Update update = new Update();

        // 2. Decrementiamo il numero totale di hotel nella città
        update.inc("city_index.hotel_count", -1);

        update.pull("top_value_hotels", Query.query(Criteria.where("hotel_name").is(hotelName)));
        // Lo togliamo da other_hotel_ids (Linking nel DB City)
        update.pull("other_hotel_ids", hotelId);

        mongoTemplate.updateFirst(query, update, "cities");
    }
}
