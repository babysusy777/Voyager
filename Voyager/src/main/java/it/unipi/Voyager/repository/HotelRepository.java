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
                        sp: { $ifNull: ['$guestStats.seasonality.counts.spring', 0] },
                        su: { $ifNull: ['$guestStats.seasonality.counts.summer', 0] },
                        au: { $ifNull: ['$guestStats.seasonality.counts.autumn', 0] },
                        wi: { $ifNull: ['$guestStats.seasonality.counts.winter', 0] }
                    }}
                    """,
            """
                    { $project: {
                        hotel_name: 1,
                        spring: '$sp', summer: '$su', autumn: '$au', winter: '$wi',
                        total: { $add: ['$sp', '$su', '$au', '$wi'] },
                        peak_visits: { $max: ['$sp', '$su', '$au', '$wi'] }
                    }}
                    """,
            """
                    { $project: {
                        hotel_name: 1,
                    
                        concentration_ratio: { 
                            $cond: [ { $eq: ['$total', 0] }, 0, { $divide: ['$peak_visits', '$total'] } ] 
                        },
                        peak_season: { $switch: { branches: [
                            { case: { $and: [ { $gt: ['$peak_visits', 0] }, { $eq: ['$peak_visits', '$spring'] } ] }, then: 'spring' },
                            { case: { $and: [ { $gt: ['$peak_visits', 0] }, { $eq: ['$peak_visits', '$summer'] } ] }, then: 'summer' },
                            { case: { $and: [ { $gt: ['$peak_visits', 0] }, { $eq: ['$peak_visits', '$autumn'] } ] }, then: 'autumn' },
                            { case: { $and: [ { $gt: ['$peak_visits', 0] }, { $eq: ['$peak_visits', '$winter'] } ] }, then: 'winter' }
                        ], default: 'no visits' }}
                    }}
                    """,
            """
                    { $project: {
                        hotel_name: 1,
                        peak_season: 1,
                        concentration_ratio: 1,
                        risk_label: { $switch: { branches: [
                            { case: { $eq: ['$peak_season', 'no visits'] }, then: 'no data' },
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
            "{ $match: { 'cityName': ?0 } }",
            "{ $group: { " +
                    "_id: '$cityName', " +
                    "hCount: { $sum: 1 }, " +
                    "vTot: { $sum: { $ifNull: ['$guestStats.totalVisits', 0] } } " +
                    "} }",
            "{ $project: { " +
                    "_id: 0, " +
                    "cityName: '$_id', " +
                    "hotelCount: '$hCount', " +
                    "totalCityVisits: '$vTot', " +
                    "demandRatio: { $cond: [ { $eq: ['$hCount', 0] }, 0, { $divide: ['$vTot', '$hCount'] } ] } " +
                    "} }",
            "{ $project: { " +
                    "cityName: 1, hotelCount: 1, totalCityVisits: 1, demandRatio: 1, " +
                    "status: { $switch: { " +
                    "branches: [ " +
                    "{ case: { $gt: ['$demandRatio', 5] }, then: 'UNDERSUPPLIED' }, " +
                    "{ case: { $lt: ['$demandRatio', 0.5] }, then: 'OVERSUPPLIED' } " +
                    "], default: 'BALANCED' " +
                    "} } " +
                    "} }"
    })
    CityIndexDTO getCityIndex(String cityName);
}