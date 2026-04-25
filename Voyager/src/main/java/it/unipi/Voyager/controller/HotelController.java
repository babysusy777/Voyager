package it.unipi.Voyager.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.unipi.Voyager.dto.FacilitiesGapDTO;
import it.unipi.Voyager.dto.HotelConcentrationDTO;
import it.unipi.Voyager.dto.HotelSearchDTO;
import it.unipi.Voyager.repository.HotelRepository;
import it.unipi.Voyager.service.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    @Autowired
    private HotelService hotelService;

    @Autowired
    private HotelRepository hotelRepository;

    @Operation(summary = "Hotels concentration index",
            description = "Returns a supply-demand analysis for a given city, computed as the ratio of total guest visits to number of hotels (demandRatio). Cities are classified as UNDERSUPPLIED (demandRatio > 5, high demand relative to supply), OVERSUPPLIED (demandRatio < 0.5, too many hotels for actual demand), or BALANCED otherwise. Useful for hosts evaluating market saturation before opening a new property.")
    @GetMapping("/host/hotel-concentration")
    public ResponseEntity<?> getHotelConcentration(@RequestParam String cityName) {
        try {
            HotelConcentrationDTO index = hotelService.getHotelConcentration(cityName);
            return ResponseEntity.ok(index);
        } catch (RuntimeException e) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Search hotel by name and city",
            description = "Returns full hotel details.")
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


}
