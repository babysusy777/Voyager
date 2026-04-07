package it.unipi.Voyager.controller;

import it.unipi.Voyager.dto.TravellerConfigRequest;
import it.unipi.Voyager.model.Traveller;
import it.unipi.Voyager.repository.TravellerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/traveller")
public class TravellerController {

    @Autowired
    private TravellerRepository travellerRepository;

    @PostMapping("/configure")
    public ResponseEntity<?> saveTravellerConfiguration(@RequestBody TravellerConfigRequest request) {
        try {
            Traveller traveller = travellerRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User does not exist"));

            // Personal info
            traveller.setCountry(request.getCountry());
            traveller.setGender(request.getGender());
            traveller.setAge(request.getAge());

            // Preferences
            Traveller.Preferences prefs = new Traveller.Preferences();
            prefs.setBudget(request.getBudget());
            prefs.setTravelType(request.getTravelType());
            prefs.setSeason(request.getSeason());
            traveller.setPreferences(prefs);

            travellerRepository.save(traveller);

            return ResponseEntity.ok("Configuration saved successfully");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}