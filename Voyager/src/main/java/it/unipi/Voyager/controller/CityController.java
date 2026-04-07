package it.unipi.Voyager.controller;

import it.unipi.Voyager.model.City;
import it.unipi.Voyager.repository.CityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    @Autowired
    private CityRepository cityRepository;


    @GetMapping("/search")
    public ResponseEntity<City> getCityByName(@RequestParam String name) {
        return cityRepository.findByName(name)
                .map(ResponseEntity::ok) // 200 OK
                .orElse(ResponseEntity.notFound().build()); //  404 Not Found se non esiste
    }
}
