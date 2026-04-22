package it.unipi.Voyager.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.Voyager.dto.TravelHabitDTO;
import it.unipi.Voyager.dto.TravellerConfigRequest;
import it.unipi.Voyager.dto.TripFrequencyDTO;
import it.unipi.Voyager.dto.RecommendationDTO;
import it.unipi.Voyager.model.Traveller;
import it.unipi.Voyager.dto.TravellerSegmentDTO;
import it.unipi.Voyager.model.graph.TravellerNode;
import it.unipi.Voyager.repository.TravellerRepository;
import it.unipi.Voyager.repository.graph.TravellerGraphRepository;
import it.unipi.Voyager.service.TravellerService;
import it.unipi.Voyager.service.graph.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traveller")
public class TravellerController {

    @Autowired
    private TravellerService travellerService;

    @Autowired
    private TravellerRepository travellerRepository;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private TravellerGraphRepository travellerGraphRepository;

    @Operation(summary = "Configure traveller's preferences",
            description = "travelType value: RELAX, ADVENTURE, CULTURAL, BUSINESS, FAMILY, NIGHTLIFE \n" +
                    "budget value: low, medium, high")
    @PostMapping("/configure")
    public ResponseEntity<?> saveTravellerConfiguration(@RequestBody TravellerConfigRequest request) {
        try {
            Traveller traveller = travellerService.setPreferences(request);
            return ResponseEntity.ok(traveller);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "View behavioral segment of the user",
            description = "budget-hunter, comfort-seeker, explorer, upgrader (referring to the hotel stars)")
    @GetMapping("/segment")
    public ResponseEntity<TravellerSegmentDTO> getSegment(@RequestParam String email) {
        TravellerSegmentDTO result = travellerService.getTravellerSegment(email);
        if (result == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "View travel habits of the user",
            description = "Returns aggregated travel stats: most frequent season, total trips, average rating given, and number of distinct cities visited.")
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

    @Operation(summary = "Trip frequency and churn analysis",
            description = "Returns avg gap between trips (days), days since last trip, churn score (0-1, probability of user abandonment based on inactivity relative to personal travel frequency), and relative status (active/at-risk/churned).")
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

    @Operation(summary = "Find similar travellers",
            description = "Returns a list of travellers most similar to the given user, based on travel type, preferences and behavioral segment (computed via Neo4j SIMILAR_TO relationships).")
    @GetMapping("/similar-friends")
    public ResponseEntity<List<TravellerNode>> getSimilarTravellers(@RequestParam String email) {
        List<TravellerNode> similarOnes = recommendationService.getSuggestions(email);

        if (similarOnes.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(similarOnes);
    }

    @Operation(summary = "City and Hotel recommendations for the traveller",
            description = "Returns a ranked list of hotels based on travel history.")
    public ResponseEntity<List<RecommendationDTO>> getRecommendations(@RequestParam String email) {
        travellerGraphRepository.computeAndSaveSimilarity(email);
        List<RecommendationDTO> recommendations = travellerGraphRepository.getPersonalizedRecommendations(email);
        if (recommendations.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(recommendations);
    }

}
