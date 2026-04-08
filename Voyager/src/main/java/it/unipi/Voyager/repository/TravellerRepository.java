package it.unipi.Voyager.repository;

import it.unipi.Voyager.dto.TravelHabitDTO;
import it.unipi.Voyager.dto.TripFrequencyDTO;
import it.unipi.Voyager.model.Traveller;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TravellerRepository extends MongoRepository<Traveller, ObjectId> {
     Optional<Traveller> findByEmail(String email);

     boolean existsByEmail(String email);

    @Aggregation(pipeline = {
            "{ $match: { email: ?0 } }",
            "{ $unwind: '$past_trips' }",
            """
            { $group: {
                _id: '$_id',
                unique_cities:  { $addToSet: '$past_trips.city' },
                avg_stars:      { $avg: '$past_trips.hotel_stars' },
                rating_std:     { $stdDevPop: '$past_trips.rating_given' },
                total_trips:    { $sum: 1 },
                trips_to_known: { $sum: { $cond: ['$past_trips.is_repeat_city', 1, 0] } }
            }}
            """,
            """
            { $project: {
                unique_cities: { $size: '$unique_cities' },
                avg_stars: 1,
                rating_std: 1,
                repeat_ratio: { $divide: ['$trips_to_known', '$total_trips'] }
            }}
            """,
            """
            { $project: {
                unique_cities: 1,
                avg_stars: 1,
                rating_std: 1,
                repeat_ratio: 1,
                segment: { $switch: { branches: [
                    { case: { $and: [{ $gt: ['$unique_cities', 4] }, { $lt: ['$repeat_ratio', 0.2] }] }, then: 'explorer' },
                    { case: { $and: [{ $gt: ['$repeat_ratio', 0.5] }, { $gt: ['$avg_stars', 3] }] }, then: 'comfort-seeker' },
                    { case: { $lt: ['$avg_stars', 2.5] }, then: 'budget-hunter' }
                ], default: 'upgrader' }}
            }}
            """
    })
    it.unipi.Voyager.dto.TravellerSegmentDTO computeSegment(String email);

    @Aggregation(pipeline = {
            "{ $match: { _id: ?0 } }",
            "{ $unwind: '$past_trips' }",
            // Step 1: Raggruppiamo per utente e stagione per contare le frequenze stagionali
            "{ $group: { " +
                    "_id: { userId: '$_id', season: '$past_trips.season' }, " +
                    "seasonCount: { $sum: 1 }, " +
                    "ratings: { $push: '$past_trips.rating_given' }, " +
                    "countries: { $addToSet: '$past_trips.country' }, " +
                    "cities: { $addToSet: '$past_trips.city' } " +
                    "} }",
            "{ $sort: { 'seasonCount': -1 } }",
            // Step 2: Raggruppiamo per utente per consolidare i risultati finali
            "{ $group: { " +
                    "_id: '$_id.userId', " +
                    "mostFrequentSeason: { $first: '$_id.season' }, " +
                    "totalTrips: { $sum: '$seasonCount' }, " +
                    "allRatings: { $push: '$ratings' }, " +
                    "allCountries: { $push: '$countries' }, " +
                    "allCities: { $push: '$cities' } " +
                    "} }",
            // Step 3: Pulizia finale e calcolo medie/dimensioni
            "{ $project: { " +
                    "mostFrequentSeason: 1, " +
                    "totalTrips: 1, " +
                    "avgRating: { $avg: { $reduce: { input: '$allRatings', initialValue: [], in: { $concatArrays: ['$$value', '$$this'] } } } }, " +
                    "countCountries: { $size: { $reduce: { input: '$allCountries', initialValue: [], in: { $setUnion: ['$$value', '$$this'] } } } }, " +
                    "countCities: { $size: { $reduce: { input: '$allCities', initialValue: [], in: { $setUnion: ['$$value', '$$this'] } } } } " +
                    "} }"
    })
    TravelHabitDTO getTravelHabits(ObjectId travellerId);
    @Aggregation(pipeline = {
            "{ $match: { _id: ?0 } }",
            "{ $project: { " +
                    "sorted_trips: { $sortArray: { input: '$past_trips', sortBy: { date: 1 } } } " +
                    "} }",
            "{ $project: { " +
                    "last_trip_date: { $last: '$sorted_trips.date' }, " +
                    "gaps: { $map: { " +
                    "input: { $range: [1, { $size: '$sorted_trips' }] }, " +
                    "as: 'i', " +
                    "in: { $subtract: [ " +
                    "{ $arrayElemAt: ['$sorted_trips.date', '$$i'] }, " +
                    "{ $arrayElemAt: ['$sorted_trips.date', { $subtract: ['$$i', 1] }] } " +
                    "] } " +
                    "} } " +
                    "} }",
            "{ $project: { " +
                    "avg_gap_days: { $divide: [{ $avg: '$gaps' }, 86400000] }, " +
                    "days_since_last: { $divide: [ " +
                    "{ $subtract: [new Date(), '$last_trip_date'] }, 86400000 " +
                    "] } " +
                    "} }",
            "{ $project: { " +
                    "avgGapDays: '$avg_gap_days', " +
                    "daysSinceLastTrip: '$days_since_last', " +
                    "churnScore: { $divide: ['$days_since_last', '$avg_gap_days'] }, " +
                    "status: { $switch: { " +
                    "branches: [ " +
                    "{ case: { $gt: [{ $divide: ['$days_since_last', '$avg_gap_days'] }, 2] }, then: 'at_risk' }, " +
                    "{ case: { $gt: [{ $divide: ['$days_since_last', '$avg_gap_days'] }, 1.2] }, then: 'slowing' } " +
                    "], default: 'active' " +
                    "} } " +
                    "} }"
    })
    TripFrequencyDTO getTripFrequency(ObjectId travellerId);
}
