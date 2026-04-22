package it.unipi.Voyager.dto;

public class TripSummaryDTO {
    private String hotelName;
    private String cityName;
    private int ratingGiven;

    public TripSummaryDTO(String hotelName, String cityName, int ratingGiven) {
        this.hotelName = hotelName;
        this.cityName = cityName;
        this.ratingGiven = ratingGiven;
    }

    public String getHotelName() { return hotelName; }
    public String getCityName() { return cityName; }
    public int getRatingGiven() { return ratingGiven; }
}