package it.unipi.Voyager.service;

import it.unipi.Voyager.dto.CityIndexDTO;
import it.unipi.Voyager.dto.FacilitiesGapDTO;
import it.unipi.Voyager.model.Hotel;
import it.unipi.Voyager.repository.HotelRepository;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class HotelService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private HotelRepository hotelRepository;

    public void updateCityCategoryAvgVisits(String cityName, String hotelRating) {

        List<Document> pipeline = Arrays.asList(

                new Document("$match", new Document("cityName", cityName)
                        .append("HotelRating", hotelRating)),


                new Document("$group", new Document("_id", null)
                        .append("peer_avg_visits", new Document("$avg", "$guestStats.totalVisits"))
                        .append("hotels", new Document("$push", "$_id"))),


                new Document("$unwind", "$hotels"),


                new Document("$project", new Document("_id", "$hotels")
                        .append("city_avg", "$peer_avg_visits")),


                new Document("$merge", new Document("into", "hotels")
                        .append("on", "_id")
                        .append("whenMatched", Arrays.asList(
                                new Document("$set", new Document("guestStats.city_category_avg_visits", "$$new.city_avg"))
                        )))
        );


        mongoTemplate.getCollection("hotels").aggregate(pipeline).toCollection();

        System.out.println("Update completato per " + cityName + " [" + hotelRating + "]");
    }

    public FacilitiesGapDTO getFacilitiesGap(String hotelName, String cityName) {
        Hotel hotel = hotelRepository.findByHotelNameAndCityName(hotelName, cityName)
                .orElseThrow(() -> new RuntimeException("Hotel not found"));

        return hotelRepository.getFacilitiesGap(
                hotel.getCityName(),
                hotel.getHotelRating(),
                4.3,
                hotel.getFacilities()
        );
    }
    public CityIndexDTO getCityIndex(String cityName) {
        // Chiamata diretta alla repository con il parametro di filtro
        CityIndexDTO index = hotelRepository.getCityPressureIndexWithIndex(cityName);

        // Se la repository restituisce null (nessun hotel trovato per quella città)
        if (index == null) {
            throw new RuntimeException("Dati non disponibili per la città: " + cityName);
        }

        return index;
    }
}
