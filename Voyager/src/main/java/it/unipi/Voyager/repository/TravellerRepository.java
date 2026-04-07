package it.unipi.Voyager.repository;

import com.mongodb.client.MongoIterable;
import it.unipi.Voyager.model.Traveller;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

 @Repository
    public interface TravellerRepository extends MongoRepository<Traveller, ObjectId> {

        Optional<Traveller> findByEmail(String email);

        boolean existsByEmail(String email);


     MongoIterable<Object> findByUserId(String userId);
 }
