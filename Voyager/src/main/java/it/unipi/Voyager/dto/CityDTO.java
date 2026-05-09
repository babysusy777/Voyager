package it.unipi.Voyager.dto;

import java.util.List;

public record CityDTO(
        String name,
        String costOfLiving,
        String safety,
        String bestTimeToVisit,
        List<HotelSummaryDTO> Hotels
) {
    public record HotelSummaryDTO(
            String hotelName,
            String stars,
            double avgPrice
    ) {}
}
