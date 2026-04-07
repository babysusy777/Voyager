package it.unipi.Voyager.repository;

import it.unipi.Voyager.model.City;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends MongoRepository<City, ObjectId> {

    // Ricerca per nome esatto della città (utile per il linking con il Grafo)
    Optional<City> findByName(String name);

    // Ricerca città per nazione
    List<City> findByCountry(String country);


    List<City> findByAvgBudget(String avgBudget); // (low, medium, high)

    // List<City> findBySafetyIndexGreaterThan(double safetyIndex);

    /**
     * TODO: Qui aggiungeremo le Aggregation Pipeline per:
     * 1. Calcolare il City Index (demand vs supply)
     * 2. Calcolare la Destination Affinity (cluster di visitatori)
     * 3. Estrarre le Top Attractions calcolate dal Grafo
     */
}