package it.unipi.Voyager.dto;

public class TripSummaryDTO {
    private String hotelName;
    private String cityName;
    private int ratingGiven;
    private String budget;

    public TripSummaryDTO(String hotelName, String cityName, int ratingGiven, String budget) {
        this.hotelName = hotelName;
        this.cityName = cityName;
        this.ratingGiven = ratingGiven;
        this.budget = budget;
    }

    public String getHotelName() { return hotelName; }
    public String getCityName() { return cityName; }
    public int getRatingGiven() { return ratingGiven; }
    public String getBudget() { return budget; }
}