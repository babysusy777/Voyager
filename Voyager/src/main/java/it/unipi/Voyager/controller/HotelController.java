package it.unipi.Voyager.controller;

import it.unipi.Voyager.dto.FacilitiesGapDTO;
import it.unipi.Voyager.dto.HotelSearchDTO;
import it.unipi.Voyager.model.Hotel;
import it.unipi.Voyager.repository.HotelRepository;
import it.unipi.Voyager.service.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    @Autowired
    private HotelService hotelService;

    @Autowired
    private HotelRepository hotelRepository;

    // Per la versione con campo precalcolato
    @PostMapping("/host/refresh-averages")
    public ResponseEntity<String> refreshCityCategoryAverages(
            @RequestParam String city,
            @RequestParam String rating) {

        try {

            hotelService.updateCityCategoryAvgVisits(city, rating);

            return ResponseEntity.ok("Aggiornamento completato con successo per " + city + " (" + rating + ").");
        } catch (Exception e) {
            // Gestione errore generico
            return ResponseEntity.internalServerError()
                    .body("Errore durante l'aggiornamento: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<HotelSearchDTO> getHotelByNameAndCityName(@RequestParam String hotelName, @RequestParam String cityName) {
        return hotelRepository.findByHotelNameAndCityName(hotelName, cityName)
                .map(h -> {
                    HotelSearchDTO dto = new HotelSearchDTO();
                    dto.setHotelName(h.getHotelName());
                    dto.setCityName(h.getCityName());
                    dto.setHotelRating(h.getHotelRating());
                    dto.setAveragePrice(h.getAveragePrice());
                    dto.setDescription(h.getDescription());
                    dto.setFacilities(h.getFacilities());

                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/host/facilities-gap")
    public ResponseEntity<FacilitiesGapDTO> getFacilitiesGap(@RequestParam String hotelName, @RequestParam String cityName) {
        FacilitiesGapDTO result = hotelService.getFacilitiesGap(hotelName, cityName);
        if (result == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(result);
    }
}
