package it.unipi.Voyager.dto;

import java.util.Map;

public class TypicalGuestProfileDTO {

    private String hotelId;
    private String hotelName;
    private String cityName;

    private int totalVisits;

    private String dominantSegment;
    private Map<String, Double> segmentDistribution;

    private String dominantSeason;
    private Map<String, Integer> seasonality;

    private double cityCategoryAvgVisits;
    private double visibilityGap;

    public TypicalGuestProfileDTO(String hotelId,
                                  String hotelName,
                                  String cityName,
                                  int totalVisits,
                                  String dominantSegment,
                                  Map<String, Double> segmentDistribution,
                                  String dominantSeason,
                                  Map<String, Integer> seasonality,
                                  double cityCategoryAvgVisits,
                                  double visibilityGap) {
        this.hotelId = hotelId;
        this.hotelName = hotelName;
        this.cityName = cityName;
        this.totalVisits = totalVisits;
        this.dominantSegment = dominantSegment;
        this.segmentDistribution = segmentDistribution;
        this.dominantSeason = dominantSeason;
        this.seasonality = seasonality;
        this.cityCategoryAvgVisits = cityCategoryAvgVisits;
        this.visibilityGap = visibilityGap;
    }

    public String getHotelId() {
        return hotelId;
    }

    public String getHotelName() {
        return hotelName;
    }

    public String getCityName() {
        return cityName;
    }

    public int getTotalVisits() {
        return totalVisits;
    }

    public String getDominantSegment() {
        return dominantSegment;
    }

    public Map<String, Double> getSegmentDistribution() {
        return segmentDistribution;
    }

    public String getDominantSeason() {
        return dominantSeason;
    }

    public Map<String, Integer> getSeasonality() {
        return seasonality;
    }

    public double getCityCategoryAvgVisits() {
        return cityCategoryAvgVisits;
    }

    public double getVisibilityGap() {
        return visibilityGap;
    }
}
