package it.unipi.Voyager.controller;

import it.unipi.Voyager.dto.TripDTO;
import it.unipi.Voyager.service.TravellerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    @Autowired
    private TravellerService travellerService;

     // Endpoint per aggiungere un nuovo viaggio o aggiornarne uno esistente
     // per un determinato utente.

    @PostMapping("/upsert/{userId}")
    public ResponseEntity<String> upsertTrip(
            @PathVariable String userId,
            @RequestBody TripDTO tripDto) {

        try {
            travellerService.upsertTrip(userId, tripDto);

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
}
