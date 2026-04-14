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
    Optional<City> findByName(String cityName);

    /*
     * TODO: Qui aggiungeremo le Aggregation Pipeline per:
     * 1. Calcolare la Destination Affinity (cluster di visitatori)
     * 2. Estrarre le Top Attractions calcolate dal Grafo
     */
}