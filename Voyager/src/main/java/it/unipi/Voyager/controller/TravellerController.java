package it.unipi.Voyager.controller;

import it.unipi.Voyager.dto.TravelHabitDTO;
import it.unipi.Voyager.dto.TravellerConfigRequest;
import it.unipi.Voyager.dto.TripFrequencyDTO;
import it.unipi.Voyager.model.Traveller;
import it.unipi.Voyager.dto.TravellerSegmentDTO;
import it.unipi.Voyager.repository.TravellerRepository;
import it.unipi.Voyager.service.TravellerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/traveller")
public class TravellerController {

    @Autowired
    private TravellerService travellerService;

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

    @GetMapping("/segment")
    public ResponseEntity<TravellerSegmentDTO> getSegment(@RequestParam String email) {
        TravellerSegmentDTO result = travellerService.getTravellerSegment(email);
        if (result == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/habits")
    public ResponseEntity<TravelHabitDTO> getTravelHabits(@RequestParam String email) {
        try {
            TravelHabitDTO habits = travellerService.getTravelHabitsByEmail(email);
            return ResponseEntity.ok(habits);
        } catch (RuntimeException e) {
            // Se l'utente non viene trovato o l'email è errata
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/frequency-analysis")
    public ResponseEntity<?> getChurnAnalysis(@RequestParam String email) {
        try {
            TripFrequencyDTO analysis = travellerService.getTripFrequencyByEmail(email);
            return ResponseEntity.ok(analysis);
        } catch (RuntimeException e) {
            // Gestione specifica per utente non trovato o viaggi insufficienti
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
