package it.unipi.Voyager.controller;

import it.unipi.Voyager.dto.AttractionDiscoveryDTO;
import it.unipi.Voyager.dto.AttractionCentralityDTO;
import it.unipi.Voyager.dto.CityDTO;
import it.unipi.Voyager.dto.CityIndexDTO;
import it.unipi.Voyager.model.City;
import it.unipi.Voyager.repository.CityRepository;
import it.unipi.Voyager.repository.graph.TravellerGraphRepository;
import it.unipi.Voyager.service.AttractionService;
import it.unipi.Voyager.service.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private HotelService hotelService;

    @Autowired
    private TravellerGraphRepository travellerGraphRepository;

    @Autowired
    private AttractionService attractionService;

    @GetMapping("/traveller/search")
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
    @GetMapping("/host/city-index")
    public ResponseEntity<?> getCityIndex(@RequestParam String cityName) {
        try {
            CityIndexDTO index = hotelService.getCityIndex(cityName);
            return ResponseEntity.ok(index);
        } catch (RuntimeException e) {
            // Restituisce 404 se la città non esiste o non ha dati, con il messaggio del Service
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            // Restituisce 500 per errori imprevisti (es. database offline)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

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

    @GetMapping("/top-attractions")
    public ResponseEntity<List<AttractionCentralityDTO>> getTopAttractions(
            @RequestParam String cityName) {
        List<AttractionCentralityDTO> result = attractionService.getTopAttractions(cityName);
        if (result == null || result.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(result);
    }
}
