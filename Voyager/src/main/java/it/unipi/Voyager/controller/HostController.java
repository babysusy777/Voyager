package it.unipi.Voyager.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.Voyager.config.Neo4jSyncService;
import it.unipi.Voyager.dto.*;
import it.unipi.Voyager.model.City;
import it.unipi.Voyager.repository.CityRepository;
import it.unipi.Voyager.repository.HostRepository;
import it.unipi.Voyager.repository.HotelRepository;
import it.unipi.Voyager.service.CityService;
import it.unipi.Voyager.service.HostService;
import it.unipi.Voyager.service.HotelService;
import it.unipi.Voyager.service.graph.CityGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    private HotelService hotelService;

    @Autowired
    private HostRepository hostRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private HostService hostService;

    @Autowired
    private Neo4jSyncService neo4jSyncService;

    @Autowired
    private CityService cityService;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Operation(summary = "Add a new hotel",
            description = "Creates a new hotel associated with the authenticated host.")
    @PostMapping("/add-hotel")
    public ResponseEntity<?> addHotel(@RequestBody HostHotelRequest request) {
        try {
            // 1. Trova l'host
            Host host = hostRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Host not found"));

            // 2. Trova la città (Assicurati di avere un CityRepository)
            City city = cityRepository.findByName(request.getCityName())
                    .orElseThrow(() -> new RuntimeException("City not found"));

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

            // 5. Update City: Document Linking & Stats
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("cityName").is(request.getCityName())),
                    new Update()
                            .push("other_hotel_ids", savedHotel.getId())
                            .inc("city_index.hotel_count", 1),
                    "cities"
            );

            return ResponseEntity.ok("Hotel created and linked to Host and City successfully");

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

    @Operation(summary = "Update hotel details",
            description = "Allows a host to update one or more details of their hotel. The request body must include host email, hotelName and cityName as identifiers. All other fields (averagePrice, description, facilities, hotelRating) are optional — only non-null fields will be updated. Returns 403 if the host does not own the specified hotel.")
    @PutMapping("/update-hotel")
    public ResponseEntity<?> updateHotelInformation(@RequestBody HostHotelUpdateRequest request) {
        try {
            Host host = hostRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Host not found"));

            Host.HotelReference targetRef = host.getHotels().stream()
                    .filter(h -> h.getHotelName().equalsIgnoreCase(request.getHotelName()) &&
                            h.getCity().equalsIgnoreCase(request.getCityName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Hotel not found in your management list"));

            Hotel hotel = hotelRepository.findById(targetRef.getHotelId())
                    .orElseThrow(() -> new RuntimeException("Hotel data inconsistency: ID not found in global collection"));

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

    @Operation(summary = "Visibility gap analysis",
            description = "Compares the host's hotel total visits against the average visits of hotels in the same city and category. Returns the gap value: negative means below average, positive means above average.")
    @GetMapping("/{email}/gap-simple")
    public ResponseEntity<List<VisibilityGapDTO>> getGapSimple(@PathVariable String email) {
        List<VisibilityGapDTO> report = hostService.getGapSimple(email);

        if (report.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(report);
    }

    @Operation(summary = "Seasonal concentration analysis",
            description = "Returns the distribution of guest arrivals across seasons for the host's hotel.")
    @GetMapping("/seasonal-concentration")
    public ResponseEntity<List<SeasonalConcentrationDTO>> getSeasonalConcentration(@RequestParam String email) {
        List<SeasonalConcentrationDTO> result = hostService.getSeasonalConcentration(email);
        if (result == null || result.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Delete hotel",
            description = "Permanently deletes a hotel identified by hotelName and cityName. The operation is cascading: removes the hotel reference from the host profile, updates city-level metrics (hotel count and hybrid arrays), and finally deletes the hotel document from the main collection. Requires the host email for ownership resolution.")
    @DeleteMapping("/delete-hotel")
    public ResponseEntity<?> deleteHotel(
            @RequestParam String email,
            @RequestParam String hotelName,
            @RequestParam String cityName) {
        try {
            Host host = hostRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Host not found"));

            Host.HotelReference targetRef = host.getHotels().stream()
                    .filter(h -> h.getHotelName().equalsIgnoreCase(hotelName) &&
                            h.getCity().equalsIgnoreCase(cityName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Hotel not found in your management list. Operation aborted."));

            String hotelId = targetRef.getHotelId();

            neo4jSyncService.deleteHotelNode(hotelName, cityName);

            // 2. Aggiorno l'Host (toglie il link nel profilo)
            hostService.removeHotelReferenceFromHost(email, hotelId);

            // 3. Aggiorno la City (decrementa count e pulisce array ibridi)
            cityService.removeHotelFromCityMetrics(cityName, hotelId, hotelName);

            // 4. Elimino l'Hotel fisicamente dalla collezione principale
            hotelRepository.deleteById(hotelId);

            return ResponseEntity.ok("Hotel '" + hotelName + "' removed correctly.");

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error.");
        }
    }

    @Operation(summary = "Facilities gap analysis for the host's hotel",
            description = "Returns the list of facilities offered by competitor hotels in the same city and category that are missing from the host's hotel.")
    @GetMapping("/host/facilities-gap")
    public ResponseEntity<FacilitiesGapDTO> getFacilitiesGap(@RequestParam String email, @RequestParam String hotelName, @RequestParam String cityName) {
        FacilitiesGapDTO result = hostService.getFacilitiesGap(email, hotelName, cityName);
        if (result == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(result);
    }

}
