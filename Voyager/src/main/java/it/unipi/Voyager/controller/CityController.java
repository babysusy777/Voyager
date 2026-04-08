package it.unipi.Voyager.controller;

import it.unipi.Voyager.dto.CityIndexDTO;
import it.unipi.Voyager.model.City;
import it.unipi.Voyager.repository.CityRepository;
import it.unipi.Voyager.service.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private HotelService hotelService;


    @GetMapping("/search")
    public ResponseEntity<City> getCityByName(@RequestParam String name) {
        return cityRepository.findByName(name)
                .map(ResponseEntity::ok) // 200 OK
                .orElse(ResponseEntity.notFound().build()); //  404 Not Found se non esiste
    }
    @GetMapping("/city-intelligence")
    public ResponseEntity<CityIndexDTO> getCityIntelligence(@RequestParam String cityName) {
        try {
            CityIndexDTO index = hotelService.getCityIndex(cityName);
            return ResponseEntity.ok(index);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
