package it.unipi.Voyager.dto;

import java.util.List;

public class HotelSearchDTO {
    private String hotelName;
    private String cityName;
    private String hotelRating;
    private Double averagePrice;
    private String description;
    private List<String> facilities;

    // Costruttore vuoto (necessario per Jackson/Framework)
    public HotelSearchDTO() {
    }

    // Costruttore completo (comodo per il mapping veloce)
    public HotelSearchDTO(String hotelName, String cityName, String hotelRating,
                          Double averagePrice, String description, List<String> facilities) {
        this.hotelName = hotelName;
        this.cityName = cityName;
        this.hotelRating = hotelRating;
        this.averagePrice = averagePrice;
        this.description = description;
        this.facilities = facilities;
    }

    // --- GETTER E SETTER ---

    public String getHotelName() {
        return hotelName;
    }

    public void setHotelName(String hotelName) {
        this.hotelName = hotelName;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getHotelRating() {
        return hotelRating;
    }

    public void setHotelRating(String hotelRating) {
        this.hotelRating = hotelRating;
    }

    public Double getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(Double averagePrice) {
        this.averagePrice = averagePrice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<String> facilities) {
        this.facilities = facilities;
    }
}