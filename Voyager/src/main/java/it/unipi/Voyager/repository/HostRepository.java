package it.unipi.Voyager.repository;

import it.unipi.Voyager.model.Host;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HostRepository extends MongoRepository<Host, ObjectId> {

    Optional<Host> findByEmail(String email);

    boolean existsByEmail(String email);
}