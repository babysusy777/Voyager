package it.unipi.Voyager.controller;

import it.unipi.Voyager.dto.TrendResponseDTO;
import it.unipi.Voyager.dto.TripDTO;
import it.unipi.Voyager.model.Traveller;
import it.unipi.Voyager.service.TravellerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips/traveller")
public class TripController {

    @Autowired
    private TravellerService travellerService;

     // Endpoint per aggiungere un nuovo viaggio o aggiornarne uno esistente
     // per un determinato utente.

    @PostMapping("/upsert/{userId}")
    public ResponseEntity<String> upsertTrip(
            @PathVariable String email,
            @RequestBody TripDTO tripDto) {

        try {
            travellerService.upsertTrip(email, tripDto);

            return ResponseEntity.ok("Operazione completata: il viaggio '" +
                    tripDto.getTripName() + "' è stato sincronizzato correttamente.");

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

    // Recupera tutti i viaggi di un utente ordinati per data.
    // URL: GET /api/trips/user/{userId}

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Traveller.Trip>> getPastTrips(@PathVariable String userId) {
        List<Traveller.Trip> trips = travellerService.getTripsSortedByDate(userId);

        if (trips.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(trips);
    }

    @GetMapping("/user/{userId}/trend")
    public ResponseEntity<TrendResponseDTO> getTravelerTrend(@PathVariable String userId) {
        try {
            String trendResult = travellerService.getTravelerStarTrend(userId);


            String status = trendResult.contains("CRESCENTE") ? "CRESCENTE" :
                    trendResult.contains("DECRESCENTE") ? "DECRESCENTE" : "STABILE";

            TrendResponseDTO response = new TrendResponseDTO(userId, status, trendResult);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
