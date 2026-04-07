package it.unipi.Voyager.dto;

import it.unipi.Voyager.model.Traveller;

import java.util.List;

public class TripDTO {
    private String tripName;
    private List<String> city;
    private List<String> hotelName;
    private String season;
    private String date;
    private int ratingGiven;

    public TripDTO() {}

    // Getters e Setters
    public String getTripName() { return tripName; }
    public void setTripName(String tripName) { this.tripName = tripName; }

    public List<String> getCity() { return city; }
    public void setCity(List<String> city) { this.city = city; }

    public List<Traveller.Trip.HotelSummary> getHotelName() { return hotelName; }
    public void setHotelName(List<String> hotelName) { this.hotelName = hotelName; }

    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getRatingGiven() { return ratingGiven; }
    public void setRatingGiven(int ratingGiven) { this.ratingGiven = ratingGiven; }
}