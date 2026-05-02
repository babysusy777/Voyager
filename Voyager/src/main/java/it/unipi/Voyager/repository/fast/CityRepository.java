package it.unipi.Voyager.repository.fast;

import it.unipi.Voyager.model.City;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CityRepository extends MongoRepository<City, ObjectId> {

    // Ricerca per nome esatto della città (utile per il linking con il Grafo)
    Optional<City> findByName(String cityName);


}