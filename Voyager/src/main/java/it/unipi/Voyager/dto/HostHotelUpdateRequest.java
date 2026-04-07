package it.unipi.Voyager.dto;

import java.util.List;

public class HostHotelUpdateRequest {

    private String email;       // identifica l'host
    private String hotelId;     // identifica l'hotel da modificare
    private Double averagePrice;
    private String description;
    private List<String> facilities;
    private String hotelRating;

    // Getters & Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getHotelId() { return hotelId; }
    public void setHotelId(String hotelId) { this.hotelId = hotelId; }

    public Double getAveragePrice() { return averagePrice; }
    public void setAveragePrice(Double averagePrice) { this.averagePrice = averagePrice; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getFacilities() { return facilities; }
    public void setFacilities(List<String> facilities) { this.facilities = facilities; }

    public String getHotelRating() { return hotelRating; }
    public void setHotelRating(String hotelRating) { this.hotelRating = hotelRating; }
}