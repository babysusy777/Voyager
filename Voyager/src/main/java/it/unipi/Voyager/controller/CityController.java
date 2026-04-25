package it.unipi.Voyager.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.Voyager.dto.*;
import it.unipi.Voyager.model.City;
import it.unipi.Voyager.repository.CityRepository;
import it.unipi.Voyager.repository.graph.TravellerGraphRepository;
import it.unipi.Voyager.service.AttractionService;
import it.unipi.Voyager.service.HotelService;
import it.unipi.Voyager.service.graph.CityGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private CityGraphService cityGraphService;

    @Autowired
    private HotelService hotelService;

    @Autowired
    private TravellerGraphRepository travellerGraphRepository;

    @Autowired
    private AttractionService attractionService;

    @Operation(summary = "Search city by name",
            description = "Returns general information about a city given its exact name, including cost of living, safety notes, best time to visit, and up to 20 randomly sampled hotels. Returns 404 if the city is not found.")
    @GetMapping("/search")
    public ResponseEntity<CityDTO> getCityByName(@RequestParam String name) {
        return cityRepository.findByName(name)
                .map(this::convertToDTO) // Mappatura da City a CityDTO
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Metodo di supporto per la conversione
    private CityDTO convertToDTO(City city) {
        List<CityDTO.HotelSummaryDTO> hotelDTOs = city.getTopValueHotels().stream()
                .map(h -> new CityDTO.HotelSummaryDTO(
                        h.getHotelName(),
                        h.getHotelStars(),
                        h.getAvgPrice()
                ))
                .collect(Collectors.toList());

        return new CityDTO(
                city.getName(),
                city.getCostOfLiving(),
                city.getSafety(),
                city.getBestTimeToVisit(),
                hotelDTOs
        );
    }



    @Operation(summary = "Discovery tips for returning travellers",
            description = "For a traveller who has already visited some cities, returns attractions classified as CENTRAL_NEW (top 25% by centrality, with no past hotel stay nearby) or HIDDEN_GEM (bottom 75% by centrality, regardless of proximity to past stays).")
    @GetMapping("/traveller/discovery-tips")
    public ResponseEntity<List<AttractionDiscoveryDTO>> getDiscoveryTips(@RequestParam String email) {
        try {
            List<AttractionDiscoveryDTO> tips = travellerGraphRepository.findReturningTravelerTips(email);

            if (tips == null || tips.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(tips);
        } catch (Exception e) {
            // Se APOC non è presente, la query fallirà qui
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Top attractions by city",
            description = "Returns the most central attractions in a given city, ranked by a centrality score computed as the product of hotel sharing (how many hotels are near the attraction) and co-attraction count (how many other attractions share the same nearby hotels). Higher scores indicate attractions that are well-connected within the city's hospitality network.")
    @GetMapping("/top-attractions")
    public ResponseEntity<List<AttractionCentralityDTO>> getTopAttractions(
            @RequestParam String cityName) {
        List<AttractionCentralityDTO> result = attractionService.getTopAttractions(cityName);
        if (result == null || result.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Find similar cities",
            description = "Returns a list of cities similar to the given one, based on shared attraction categories.")
    @GetMapping("/similar-cities")
    public ResponseEntity<?> getSimilarCities(@RequestParam String cityName) {
        List<CitySimilarityDTO> similarCities = cityGraphService.getSimilarCities(cityName);

        if (similarCities.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Nessuna città simile trovata o città non esistente.");
        }

        return ResponseEntity.ok(similarCities);
    }
}
