package it.unipi.Voyager.repository;

import it.unipi.Voyager.dto.CityIndexDTO;
import it.unipi.Voyager.dto.FacilitiesGapDTO;
import it.unipi.Voyager.dto.SeasonalConcentrationDTO;
import it.unipi.Voyager.model.Hotel;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotelRepository extends MongoRepository<Hotel, String> {

    Optional<Hotel> findByHotelNameAndCityName(String hotelName, String cityName);

    @Aggregation(pipeline = {
            "{ $match: { _id: { $in: ?0 } } }",
            """
            { $project: {
                hotel_name: '$HotelName',
                spring:  '$guestStats.seasonality.counts.spring',
                summer:  '$guestStats.seasonality.counts.summer',
                autumn:  '$guestStats.seasonality.counts.autumn',
                winter:  '$guestStats.seasonality.counts.winter',
                total: { $add: ['$guestStats.seasonality.counts.spring', '$guestStats.seasonality.counts.summer',
                                 '$guestStats.seasonality.counts.autumn', '$guestStats.seasonality.counts.winter'] },
                peak_visits: { $max: ['$guestStats.seasonality.counts.spring', '$guestStats.seasonality.counts.summer',
                                       '$guestStats.seasonality.counts.autumn', '$guestStats.seasonality.counts.winter'] }
            }}
            """,
            """
            { $project: {
                hotel_name: 1,
                concentration_ratio: { $divide: ['$peak_visits', '$total'] },
                peak_season: { $switch: { branches: [
                    { case: { $eq: ['$peak_visits', '$spring'] }, then: 'spring' },
                    { case: { $eq: ['$peak_visits', '$summer'] }, then: 'summer' },
                    { case: { $eq: ['$peak_visits', '$autumn'] }, then: 'autumn' },
                    { case: { $eq: ['$peak_visits', '$winter'] }, then: 'winter' }
                ], default: 'unknown' }}
            }}
            """,
            """
            { $project: {
                hotel_name: 1,
                peak_season: 1,
                concentration_ratio: 1,
                risk_label: { $switch: { branches: [
                    { case: { $gt: ['$concentration_ratio', 0.65] }, then: 'mono-seasonal risk' },
                    { case: { $lt: ['$concentration_ratio', 0.30] }, then: 'all-season asset' }
                ], default: 'moderate seasonality' }}
            }}
            """
    })
    List<SeasonalConcentrationDTO> getSeasonalConcentrationByIds(List<String> hotelIds);

    @Aggregation(pipeline = {
            "{ $match: { cityName: ?0, HotelRating: ?1, 'guestStats.avgRatingGiven': { $gte: ?2 } } }",
            "{ $group: { _id: null, peer_facilities: { $addToSet: '$HotelFacilities' } } }",
            "{ $project: { missing: { $setDifference: ['$peer_facilities', ?3] } } }"
    })
    FacilitiesGapDTO getFacilitiesGap(String city, String rating, double minRating, List<String> myFacilities);
    @Aggregation(pipeline = {
            // 1. IL MATCH SFRUTTA L'INDICE: Filtra istantaneamente per città e (opzionalmente) rating
            // Usiamo ?0 per cityName. Se vuoi filtrare anche per stelle, aggiungeresti ?1
            "{ $match: { 'cityName': ?0 } }",

            // 2. Raggruppamento: ora lavora solo sui documenti già estratti dall'indice
            "{ $group: { " +
                    "_id: '$cityName', " +
                    "hotelCount: { $sum: 1 }, " +
                    "totalCityVisits: { $sum: '$guestStats.totalVisits' } " +
                    "} }",

            // 3. Proiezione e calcolo del ratio
            "{ $project: { " +
                    "cityName: '$_id', " +
                    "hotelCount: 1, " +
                    "totalCityVisits: 1, " +
                    "demandRatio: { $divide: ['$totalCityVisits', '$hotelCount'] }, " +
                    "status: { $switch: { " +
                    "branches: [ " +
                    "{ case: { $gt: [{ $divide: ['$totalCityVisits', '$hotelCount'] }, 5] }, then: 'UNDERSUPPLIED' }, " +
                    "{ case: { $lt: [{ $divide: ['$totalCityVisits', '$hotelCount'] }, 0.5] }, then: 'OVERSUPPLIED' } " +
                    "], default: 'BALANCED' " +
                    "} } " +
                    "} }"
    })
    CityIndexDTO getCityPressureIndexWithIndex(String cityName);
}