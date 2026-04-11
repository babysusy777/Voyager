package it.unipi.Voyager.controller;

import it.unipi.Voyager.dto.HostHotelUpdateRequest;
import it.unipi.Voyager.dto.SeasonalConcentrationDTO;
import it.unipi.Voyager.dto.VisibilityGapDTO;
import it.unipi.Voyager.repository.HostRepository;
import it.unipi.Voyager.repository.HotelRepository;
import it.unipi.Voyager.service.HostService;
import it.unipi.Voyager.service.graph.CityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import it.unipi.Voyager.dto.HostHotelRequest;
import it.unipi.Voyager.model.Host;
import it.unipi.Voyager.model.Hotel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/host")
public class HostController {

    @Autowired
    private CityService cityService;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private HostService hostService;

    @PostMapping("/add-hotel")
    public ResponseEntity<?> addHotel(@RequestBody HostHotelRequest request) {
        try {
            // 1. Trova l'host
            Host host = hostRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Host not found"));

            // 2. Crea il nuovo documento Hotel
            Hotel hotel = new Hotel();
            hotel.setHotelName(request.getHotelName());
            hotel.setCityName(request.getCityName());
            hotel.setHotelRating(request.getHotelRating());
            hotel.setAddress(request.getAddress());
            hotel.setAveragePrice(request.getAveragePrice());
            hotel.setDescription(request.getDescription());
            hotel.setFacilities(request.getFacilities());
            // guestStats lasciato null: nessuna visita ancora

            Hotel savedHotel = hotelRepository.save(hotel);

            // 3. Costruisci la HotelReference (partial embedding nell'host)
            Host.HotelReference ref = new Host.HotelReference();
            ref.setHotelId(savedHotel.getId());
            ref.setHotelName(savedHotel.getHotelName());
            ref.setCity(savedHotel.getCityName());
            ref.setStars(parseStars(savedHotel.getHotelRating()));

            // 4. Aggiunge e salva l'host
            if (host.getHotels() == null) host.setHotels(new ArrayList<>());
            host.getHotels().add(ref);
            hostRepository.save(host);

            return ResponseEntity.ok("Hotel created and registered successfully");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private int parseStars(String hotelRating) {
        if (hotelRating == null) return 0;
        return switch (hotelRating.toLowerCase()) {
            case "onestar"   -> 1;
            case "twostar"   -> 2;
            case "threestar" -> 3;
            case "fourstar"  -> 4;
            case "fivestar"  -> 5;
            default          -> 0;
        };
    }

    @PutMapping("/update-hotel")
    public ResponseEntity<?> updateHotelInformation(@RequestBody HostHotelUpdateRequest request) {
        try {
            // 1. Verifica che l'host esista
            Host host = hostRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Host not found"));

            // 2. Verifica che l'hotel appartenga all'host
            boolean owns = host.getHotels() != null && host.getHotels().stream()
                    .anyMatch(h -> h.getHotelId().equals(request.getHotelId()));
            if (!owns) {
                return ResponseEntity.status(403).body("Hotel not owned by this host");
            }

            // 3. Trova e aggiorna il documento Hotel
            Hotel hotel = hotelRepository.findById(request.getHotelId())
                    .orElseThrow(() -> new RuntimeException("Hotel not found"));

            if (request.getAveragePrice() != null) hotel.setAveragePrice(request.getAveragePrice());
            if (request.getDescription() != null)  hotel.setDescription(request.getDescription());
            if (request.getFacilities() != null)   hotel.setFacilities(request.getFacilities());
            if (request.getHotelRating() != null) {
                hotel.setHotelRating(request.getHotelRating());

                // 4. Aggiorna anche le stelle nella HotelReference embedded nell'host
                host.getHotels().stream()
                        .filter(h -> h.getHotelId().equals(request.getHotelId()))
                        .findFirst()
                        .ifPresent(h -> h.setStars(parseStars(request.getHotelRating())));
                hostRepository.save(host);
            }

            hotelRepository.save(hotel);

            return ResponseEntity.ok("Hotel updated successfully");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    // VERSIONE CON LOOKUP
    @GetMapping("/{username}/visibility-gap")
    public ResponseEntity<List<VisibilityGapDTO>> getVisibilityGap(@PathVariable String username) {

        List<VisibilityGapDTO> report = hostService.getHostVisibilityGap(username);

        if (report.isEmpty()) {
            // Restituiamo 204 No Content se l'host non esiste o non ha hotel
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(report);
    }

    // VERSIONE CON CAMPO PRECALCOLATO
    @GetMapping("/{username}/gap-simple")
    public ResponseEntity<List<VisibilityGapDTO>> getGapSimple(@PathVariable String username) {
        List<VisibilityGapDTO> report = hostService.getGapSimple(username);

        if (report.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(report);
    }

    @GetMapping("/seasonal-concentration")
    public ResponseEntity<List<SeasonalConcentrationDTO>> getSeasonalConcentration(@RequestParam String email) {
        List<SeasonalConcentrationDTO> result = hostService.getSeasonalConcentration(email);
        if (result == null || result.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/analysis/similar-cities")
    public ResponseEntity<?> getSimilarCities(@RequestParam String cityName) {
        List<Map<String, Object>> similarCities = cityService.getSimilarCities(cityName);

        if (similarCities.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Nessuna città simile trovata o città non esistente.");
        }

        return ResponseEntity.ok(similarCities);
    }

}
