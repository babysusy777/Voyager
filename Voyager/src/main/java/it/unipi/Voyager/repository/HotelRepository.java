package it.unipi.Voyager.repository;

import it.unipi.Voyager.model.Hotel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HotelRepository extends MongoRepository<Hotel, String> {

    Optional<Hotel> findByHotelNameAndCityName(String hotelName, String cityName);
}