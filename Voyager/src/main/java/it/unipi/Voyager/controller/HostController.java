package it.unipi.Voyager.controller;

import it.unipi.Voyager.config.Neo4jSyncService;
import it.unipi.Voyager.dto.HostHotelUpdateRequest;
import it.unipi.Voyager.dto.SeasonalConcentrationDTO;
import it.unipi.Voyager.dto.VisibilityGapDTO;
import it.unipi.Voyager.repository.HostRepository;
import it.unipi.Voyager.repository.HotelRepository;
import it.unipi.Voyager.service.HostService;
import it.unipi.Voyager.service.graph.CityGraphService;
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
    private CityGraphService cityGraphService;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private HostService hostService;

    @Autowired
    private Neo4jSyncService neo4jSyncService;

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
            neo4jSyncService.syncHotelByName(savedHotel.getHotelName(), savedHotel.getCityName());

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
            case "oneStar"   -> 1;
            case "twoStar"   -> 2;
            case "threeStar" -> 3;
            case "fourStar"  -> 4;
            case "fiveStar"  -> 5;
            default          -> 0;
        };
    }
    @PatchMapping("/{email}/hotels/{cityName}/{hotelName}")
    public ResponseEntity<?> patchHotel(
            @PathVariable String email,
            @PathVariable String cityName,
            @PathVariable String hotelName,
            @RequestBody Map<String, Object> updates) {

        try {
            hostService.updateHotelPartial(email, cityName, hotelName, updates);
            return ResponseEntity.ok("Dati hotel aggiornati con successo.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore interno.");
        }
    }

//non riesco a far modificare l'hotel nella città
    @PutMapping("/update-hotel")
    public ResponseEntity<?> updateHotelInformation(@RequestBody HostHotelUpdateRequest request) {
        try {
            // 1. Trova l'hotel per Nome e Città
            Hotel hotel = hotelRepository.findByHotelNameAndCityName(request.getHotelName(), request.getCityName())
                    .orElseThrow(() -> new RuntimeException("Hotel not found in this city"));

            // 2. Verifica che l'host esista e possieda questo specifico ID hotel
            Host host = hostRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Host not found"));

            boolean owns = host.getHotels() != null && host.getHotels().stream()
                    .anyMatch(h -> h.getHotelId().equals(hotel.getId()));

            if (!owns) {
                return ResponseEntity.status(403).body("Host couldn't modify this hotel");
            }

            // 3. Aggiorna i dati nel documento Hotel
            if (request.getAveragePrice() != null) hotel.setAveragePrice(request.getAveragePrice());
            if (request.getDescription() != null)  hotel.setDescription(request.getDescription());
            if (request.getFacilities() != null)   hotel.setFacilities(request.getFacilities());

            if (request.getHotelRating() != null) {
                hotel.setHotelRating(request.getHotelRating());

                // 4. Update Partial Embedding nell'HOST
                host.getHotels().stream()
                        .filter(h -> h.getHotelId().equals(hotel.getId()))
                        .findFirst()
                        .ifPresent(h -> h.setStars(parseStars(request.getHotelRating())));
                hostRepository.save(host);

            }

            hotelRepository.save(hotel);

            // 6. Sincronizzazione Neo4j
            neo4jSyncService.syncHotelByName(hotel.getHotelName(), hotel.getCityName());

            return ResponseEntity.ok("Hotel updated successfully");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


/*

    // VERSIONE CON LOOKUP
    @GetMapping("/{username}/visibility-gap")
    public ResponseEntity<List<VisibilityGapDTO>> getVisibilityGap(@PathVariable String username) {

        List<VisibilityGapDTO> report = hostService.getHostVisibilityGap(username);

        if (report.isEmpty()) {
            // Restituiamo 204 No Content se l'host non esiste o non ha hotel
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(report);
    } */

    // VERSIONE CON CAMPO PRECALCOLATO
    @GetMapping("/{email}/gap-simple")
    public ResponseEntity<List<VisibilityGapDTO>> getGapSimple(@PathVariable String email) {
        List<VisibilityGapDTO> report = hostService.getGapSimple(email);

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
        List<Map<String, Object>> similarCities = cityGraphService.getSimilarCities(cityName);

        if (similarCities.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Nessuna città simile trovata o città non esistente.");
        }

        return ResponseEntity.ok(similarCities);
    }

   /* @DeleteMapping("/delete-hotel")
    public ResponseEntity<?> deleteHotel(
            @RequestParam String email,
            @RequestParam String hotelName,
            @RequestParam String cityName) {
        try {
            // 1. Trova l'hotel per Nome e Città per ottenere l'ID reale
            Hotel hotel = hotelRepository.findByHotelNameAndCityName(hotelName, cityName)
                    .orElseThrow(() -> new RuntimeException("Hotel not found in this city"));

            String hotelId = hotel.getId();

            // 2. Trova l'Host e verifica che possieda questo specifico hotel
            Host host = hostRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Host not found"));

            boolean owns = host.getHotels() != null && host.getHotels().stream()
                    .anyMatch(h -> h.getHotelId().equals(hotelId));

            if (!owns) {
                return ResponseEntity.status(403).body("Host doesn't own this hotel");
            }

            // 3. RIMOZIONE DA HOST (Partial Embedding)
            host.getHotels().removeIf(h -> h.getHotelId().equals(hotelId));
            hostRepository.save(host);

            // 4. AGGIORNAMENTO CITY (Consistenza Strategia Ibrida)
            // Usiamo i parametri che abbiamo già
            updateCityAfterDeletion(cityName, hotelId, hotelName);

            // 5. RIMOZIONE DA HOTELS (Collezione principale)
            hotelRepository.deleteById(hotelId);

            // 6. SYNC NEO4J
            neo4jSyncService.syncHotelDeletion(hotelName, cityName);

            return ResponseEntity.ok("Hotel '" + hotelName + "' deleted correctly");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }*/

}
