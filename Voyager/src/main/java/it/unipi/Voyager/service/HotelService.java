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
        CityIndexDTO index = hotelRepository.getCityIndex(cityName);

        // Se la repository restituisce null (nessun hotel trovato per quella città)
        if (index == null) {
            throw new RuntimeException("Dati non disponibili per la città: " + cityName);
        }

        return index;
    }

    // Firma aggiornata in HotelService
    public void updateHotelStatsOnNewTrip(String hotelName, List<String> cities, String season, String userSegment, String tripBudget) {

        // city nel trip è una List<String>, prendi il primo elemento
        String cityName = (cities != null && !cities.isEmpty()) ? cities.get(0) : null;
        if (cityName == null) return;

        // Step 1: incrementa totalVisits e counts stagionali — ora con cityName come secondo filtro
        String seasonField = "guestStats.seasonality.counts." + season;
        mongoTemplate.getCollection("hotels").updateOne(
                new Document("HotelName", hotelName).append("cityName", cityName),
                new Document("$inc", new Document("guestStats.totalVisits", 1)
                        .append(seasonField, 1))
        );

        // Step 2: recupera rating per ricalcolare city_category_avg (cityName già noto)
        Document hotel = mongoTemplate.getCollection("hotels")
                .find(new Document("HotelName", hotelName).append("cityName", cityName))
                .projection(new Document("HotelRating", 1))
                .first();

        if (hotel == null) return;
        updateCityCategoryAvgVisits(cityName, hotel.getString("HotelRating"));

        // Step 3: ricalcola segment_distribution e preference_distribution
        updateDistributions(hotelName);

        // Step 4: aggiorna city_index
        Document cityDoc = mongoTemplate.getCollection("cities")
                .find(new Document("cityName", cityName))
                .projection(new Document("city_index", 1))
                .first();

        if (cityDoc != null) {
            Document cityIndex = cityDoc.get("city_index", Document.class);
            int hotelCount = cityIndex != null ? cityIndex.getInteger("hotel_count", 1) : 1;
            mongoTemplate.getCollection("cities").updateOne(
                    new Document("cityName", cityName),
                    Arrays.asList(
                            new Document("$set", new Document("city_index.total_visits",
                                    new Document("$add", Arrays.asList("$city_index.total_visits", 1)))),
                            new Document("$set", new Document("city_index.demand_ratio",
                                    new Document("$divide", Arrays.asList(
                                            new Document("$add", Arrays.asList("$city_index.total_visits", 1)),
                                            hotelCount
                                    ))))
                    )
            );
        }

        // Step 5: aggiorna seasonality della città
        String citySeasonField = "seasonality." + season;
        mongoTemplate.getCollection("cities").updateOne(
                new Document("cityName", cityName),
                Arrays.asList(
                        new Document("$set", new Document(citySeasonField,
                                new Document("$add", Arrays.asList("$" + citySeasonField, 1)))),
                        new Document("$set", new Document("seasonality.peak_season",
                                new Document("$switch", new Document("branches", Arrays.asList(
                                        new Document("case", new Document("$and", Arrays.asList(
                                                new Document("$gte", Arrays.asList("$seasonality.spring", "$seasonality.summer")),
                                                new Document("$gte", Arrays.asList("$seasonality.spring", "$seasonality.autumn")),
                                                new Document("$gte", Arrays.asList("$seasonality.spring", "$seasonality.winter"))
                                        ))).append("then", "spring"),
                                        new Document("case", new Document("$and", Arrays.asList(
                                                new Document("$gte", Arrays.asList("$seasonality.summer", "$seasonality.spring")),
                                                new Document("$gte", Arrays.asList("$seasonality.summer", "$seasonality.autumn")),
                                                new Document("$gte", Arrays.asList("$seasonality.summer", "$seasonality.winter"))
                                        ))).append("then", "summer"),
                                        new Document("case", new Document("$and", Arrays.asList(
                                                new Document("$gte", Arrays.asList("$seasonality.autumn", "$seasonality.spring")),
                                                new Document("$gte", Arrays.asList("$seasonality.autumn", "$seasonality.summer")),
                                                new Document("$gte", Arrays.asList("$seasonality.autumn", "$seasonality.winter"))
                                        ))).append("then", "autumn")
                                )).append("default", "winter")))),
                        new Document("$set", new Document("seasonality.concentration_ratio",
                                new Document("$divide", Arrays.asList(
                                        new Document("$max", Arrays.asList(
                                                "$seasonality.spring", "$seasonality.summer",
                                                "$seasonality.autumn", "$seasonality.winter"
                                        )),
                                        new Document("$add", Arrays.asList(
                                                "$seasonality.spring", "$seasonality.summer",
                                                "$seasonality.autumn", "$seasonality.winter",
                                                1
                                        ))
                                ))))
                )
        );
    }

    private void updateDistributions(String hotelName) {
        List<Document> pipeline = Arrays.asList(

                new Document("$unwind", "$past_trips"),

                new Document("$match", new Document("past_trips.hotel_name", hotelName)),

                new Document("$group", new Document("_id", null)
                        .append("total", new Document("$sum", 1))
                        .append("explorer",       new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$user_segment", "explorer")), 1, 0))))
                        .append("comfort_seeker", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$user_segment", "comfort-seeker")), 1, 0))))
                        .append("upgrader",       new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$user_segment", "upgrader")), 1, 0))))
                        .append("budget_hunter",  new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$user_segment", "budget-hunter")), 1, 0))))
                        .append("budget_low",    new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.trip_budget", "low")), 1, 0))))
                        .append("budget_medium", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.trip_budget", "medium")), 1, 0))))
                        .append("budget_high",   new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.trip_budget", "high")), 1, 0))))
                        .append("season_spring", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.season", "spring")), 1, 0))))
                        .append("season_summer", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.season", "summer")), 1, 0))))
                        .append("season_autumn", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.season", "autumn")), 1, 0))))
                        .append("season_winter", new Document("$sum", new Document("$cond", Arrays.asList(
                                new Document("$eq", Arrays.asList("$past_trips.season", "winter")), 1, 0))))
                ),

                // Costruisce le distribuzioni con proporzioni e dominant
                new Document("$project", new Document("_id", 0)
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
        );

        Document result = mongoTemplate.getCollection("travellers")
                .aggregate(pipeline).first();

        if (result == null) return;

        mongoTemplate.getCollection("hotels").updateOne(
                new Document("HotelName", hotelName),
                new Document("$set", new Document()
                        .append("guestStats.segment_distribution",    result.get("segment_distribution"))
                        .append("guestStats.preference_distribution", result.get("preference_distribution"))
                )
        );
    }
}
