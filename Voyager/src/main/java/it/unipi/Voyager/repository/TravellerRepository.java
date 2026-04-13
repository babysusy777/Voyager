package it.unipi.Voyager.repository;

import it.unipi.Voyager.dto.TravelHabitDTO;
import it.unipi.Voyager.dto.TripFrequencyDTO;
import it.unipi.Voyager.model.Traveller;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.neo4j.repository.query.Query;
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
                    "cities: { $addToSet: '$past_trips.city' } " +
                    "} }",
            "{ $sort: { 'seasonCount': -1 } }",
            // Step 2: Raggruppiamo per utente per consolidare i risultati finali
            "{ $group: { " +
                    "_id: '$_id.userId', " +
                    "mostFrequentSeason: { $first: '$_id.season' }, " +
                    "totalTrips: { $sum: '$seasonCount' }, " +
                    "allRatings: { $push: '$ratings' }, " +
                    "allCities: { $push: '$cities' } " +
                    "} }",
            // Step 3: Pulizia finale e calcolo medie/dimensioni
            "{ $project: { " +
                    "mostFrequentSeason: 1, " +
                    "totalTrips: 1, " +
                    "avgRating: { $avg: { $reduce: { input: '$allRatings', initialValue: [], in: { $concatArrays: ['$$value', '$$this'] } } } }, " +
                    "countCities: { $size: { $reduce: { input: '$allCities', initialValue: [], in: { $setUnion: ['$$value', '$$this'] } } } } " +
                    "} }"
    })
    TravelHabitDTO getTravelHabits(ObjectId travellerId);

    @Aggregation(pipeline = {
            // 1. Filtra per email
            "{ $match: { email: ?0 } }",

            // 2. Crea l'array di date (usiamo $addFields per mantenere il resto del documento)
            "{ $addFields: { " +
                    "dates: { $map: { " +
                    "input: { $sortArray: { input: { $ifNull: ['$past_trips', []] }, sortBy: { date: 1 } } }, " +
                    "as: 't', " +
                    "in: { $dateFromString: { dateString: '$$t.date' } } " +
                    "} } " +
                    "} }",

            // 3. Calcola i gap (con controllo di sicurezza sull'array)
            "{ $addFields: { " +
                    "last_trip_date: { $last: '$dates' }, " +
                    "gaps: { $cond: [ " +
                    "{ $gt: [{ $size: { $ifNull: ['$dates', []] } }, 1] }, " + // Se ci sono almeno 2 date
                    "{ $map: { " +
                    "input: { $range: [1, { $size: '$dates' }] }, " +
                    "as: 'i', " +
                    "in: { $subtract: [ " +
                    "{ $arrayElemAt: ['$dates', '$$i'] }, " +
                    "{ $arrayElemAt: ['$dates', { $subtract: ['$$i', 1] }] } " +
                    "] } " +
                    "} }, " +
                    "[] " + // Altrimenti array vuoto
                    "] } " +
                    "} }",

            // 4. Medie
            "{ $addFields: { " +
                    "avg_gap_days: { $cond: [ { $gt: [{ $size: '$gaps' }, 0] }, { $divide: [{ $avg: '$gaps' }, 86400000] }, 0] }, " +
                    "days_since_last: { $divide: [{ $subtract: [new Date(), '$last_trip_date'] }, 86400000] } " +
                    "} }",

            // 5. Mapping finale verso il DTO
            "{ $project: { " +
                    "_id: 0, " +
                    "avgGapDays: '$avg_gap_days', " +
                    "daysSinceLastTrip: '$days_since_last', " +
                    "churnScore: { $cond: [ { $gt: ['$avg_gap_days', 0] }, { $divide: ['$days_since_last', '$avg_gap_days'] }, 0] }, " +
                    "status: { $switch: { " +
                    "branches: [ " +
                    "{ case: { $lte: ['$avg_gap_days', 0] }, then: 'active' }, " +
                    "{ case: { $gt: [{ $divide: ['$days_since_last', { $max: ['$avg_gap_days', 1] }] }, 2] }, then: 'at_risk' }, " +
                    "{ case: { $gt: [{ $divide: ['$days_since_last', { $max: ['$avg_gap_days', 1] }] }, 1.2] }, then: 'slowing' } " +
                    "], default: 'active' " +
                    "} } " +
                    "} }"
    })
    TripFrequencyDTO getTripFrequency(String email);
}
