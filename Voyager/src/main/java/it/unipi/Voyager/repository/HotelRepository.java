package it.unipi.Voyager.repository;

import it.unipi.Voyager.dto.CityIndexDTO;
import it.unipi.Voyager.dto.FacilitiesGapDTO;
import it.unipi.Voyager.dto.SeasonalConcentrationDTO;
import it.unipi.Voyager.model.Hotel;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotelRepository extends MongoRepository<Hotel, String> {

    Optional<Hotel> findByHotelNameAndCityName(String hotelName, String cityName);

    @Aggregation(pipeline = {
            "{ $match: { _id: { $in: ?0 } } }",  // ← stage dedicato
            """
            { $project: {
                HotelName: '$HotelName',
                sp: { $ifNull: ['$guestStats.seasonality.counts.spring', 0] },
                su: { $ifNull: ['$guestStats.seasonality.counts.summer', 0] },
                au: { $ifNull: ['$guestStats.seasonality.counts.autumn', 0] },
                wi: { $ifNull: ['$guestStats.seasonality.counts.winter', 0] }
            }}
            """,
            """
            { $addFields: {
                total: { $add: ['$sp', '$su', '$au', '$wi'] },
                peak_visits: { $max: ['$sp', '$su', '$au', '$wi'] }
            }}
            """,
            """
            { $addFields: {
                concentrationRatio: { 
                    $cond: [ { $eq: ['$total', 0] }, 0, { $divide: ['$peak_visits', '$total'] } ] 
                },
                peakSeason: { $switch: { branches: [
                    { case: { $and: [ { $gt: ['$peak_visits', 0] }, { $eq: ['$peak_visits', '$sp'] } ] }, then: 'spring' },
                    { case: { $and: [ { $gt: ['$peak_visits', 0] }, { $eq: ['$peak_visits', '$su'] } ] }, then: 'summer' },
                    { case: { $and: [ { $gt: ['$peak_visits', 0] }, { $eq: ['$peak_visits', '$au'] } ] }, then: 'autumn' },
                    { case: { $and: [ { $gt: ['$peak_visits', 0] }, { $eq: ['$peak_visits', '$wi'] } ] }, then: 'winter' }
                ], default: 'no visits' }}
            }}
            """,
            """
            { $project: {
                 _id: 0,
                HotelName: 1,
                peakSeason: 1,
                concentrationRatio: 1,
                riskLabel: { $switch: { branches: [
                    { case: { $eq: ['$peakSeason', 'no visits'] }, then: 'no data' },
                    { case: { $gt: ['$concentrationRatio', 0.65] }, then: 'mono-seasonal risk' },
                    { case: { $lt: ['$concentrationRatio', 0.30] }, then: 'all-season asset' }
                ], default: 'moderate seasonality' }}
            }}
            """
    })
    List<SeasonalConcentrationDTO> getSeasonalConcentrationByIds(List<ObjectId> hotelIds);

    @Aggregation(pipeline = {
            "{ $match: { " +
                    "cityName: { $regex: ?0, $options: 'i' }, " +
                    "HotelRating: { $regex: ?1, $options: 'i' }, " +
                    "HotelName: { $ne: ?2 } " +
                    "} }",
            "{ $unwind: '$HotelFacilities' }",
            "{ $group: { _id: null, peerFacilities: { $addToSet: '$HotelFacilities' } } }",
            "{ $project: { _id: 0, missing: { $setDifference: ['$peerFacilities', ?3] } } }"
    })
    FacilitiesGapDTO getFacilitiesGap(String city, String rating, String hotelName, List<String> myFacilities);

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