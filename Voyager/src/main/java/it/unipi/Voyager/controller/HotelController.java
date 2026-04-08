package it.unipi.Voyager.controller;

import it.unipi.Voyager.service.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    @Autowired
    private HotelService hotelService;

    // Per la versione con campo precalcolato
    @PostMapping("/refresh-averages")
    public ResponseEntity<String> refreshCityCategoryAverages(
            @RequestParam String city,
            @RequestParam String rating) {

        try {
            // Chiamata al metodo Plain Language nel Service
            hotelService.updateCityCategoryAvgVisits(city, rating);

            return ResponseEntity.ok("Aggiornamento completato con successo per " + city + " (" + rating + ").");
        } catch (Exception e) {
            // Gestione errore generico
            return ResponseEntity.internalServerError()
                    .body("Errore durante l'aggiornamento: " + e.getMessage());
        }
    }
}
