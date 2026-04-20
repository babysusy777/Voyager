package it.unipi.Voyager.controller;

import it.unipi.Voyager.dto.TrendResponseDTO;
import it.unipi.Voyager.dto.TripDTO;
import it.unipi.Voyager.model.Traveller;
import it.unipi.Voyager.repository.TravellerRepository;
import it.unipi.Voyager.service.TravellerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trips/traveller")
public class TripController {

    @Autowired
    private TravellerService travellerService;

    @Autowired
    private TravellerRepository travellerRepository;

     // Endpoint per aggiungere un nuovo viaggio o aggiornarne uno esistente
     // per un determinato utente.

    @PatchMapping("/{email}/trips/{tripName}")
    public ResponseEntity<String> updateTripFields(
            @PathVariable String email,
            @PathVariable String tripName,
            @RequestBody Map<String, Object> updates) {

        try {
            travellerService.updateTripPartial(email, tripName, updates);
            return ResponseEntity.ok("Viaggio '" + tripName + "' aggiornato con successo.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Errore durante l'aggiornamento: " + e.getMessage());
        }
    }

    @GetMapping("/{email}/trips/{tripName}")
    public ResponseEntity<?> getSingleTrip(@PathVariable String email, @PathVariable String tripName) {
        // Utilizziamo un'aggregazione o una query mirata per restituire solo il viaggio interessato
        // Qui un esempio semplificato tramite stream se hai già il Traveller caricato
        return travellerRepository.findByEmail(email)
                .map(t -> t.getTrips().stream()
                        .filter(trip -> trip.getTripName().equals(tripName))
                        .findFirst()
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build()))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/{email}/upsert")
    public ResponseEntity<String> upsertTrip(
            @PathVariable String email,
            @RequestBody TripDTO tripDto) {

        try {
            travellerService.upsertTrip(email, tripDto);

            return ResponseEntity.ok("Operation completed: the trip '" +
                    tripDto.getTripName() + "' has been synchronized successfully.");

        } catch (IllegalArgumentException e) {
            // Gestione caso utente non trovato o dati non validi
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Data error: " + e.getMessage());
        } catch (Exception e) {
            // Gestione errore generico
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal error.");
        }
    }

    // DELETE TRIP
//    @DeleteMapping("/{email}/{tripName}")
//    public ResponseEntity<String> deleteTrip(
//            @PathVariable String email,
//            @PathVariable String tripName) {
//
//        try {
//            travellerService.deleteTrip(email, tripName);
//            return ResponseEntity.ok("Viaggio '" + tripName + "' rimosso correttamente.");
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
//        }
//    }

    // Recupera tutti i viaggi di un utente ordinati per data.

    @GetMapping("/{email}/trip")
    public ResponseEntity<List<Traveller.Trip>> getPastTrips(@PathVariable String email) {
        List<Traveller.Trip> trips = travellerService.getTripsSortedByDate(email);

        if (trips.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(trips);
    }

    @GetMapping("/{email}/trend")
    public ResponseEntity<TrendResponseDTO> getTravelerTrend(@PathVariable String email) {
        try {
            String trendResult = travellerService.getTravelerStarTrend(email);

            TrendResponseDTO response = new TrendResponseDTO(email, trendResult);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}