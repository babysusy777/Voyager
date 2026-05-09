package it.unipi.Voyager.dto;

import java.util.List;

public class QualityPriceHotelDTO {

    private String hotelId;
    private String hotelName;
    private String cityName;
    private String hotelRating;

    private int stars;
    private int facilitiesCount;
    private double averagePrice;
    private double qualityPriceRatio;

    private List<String> facilities;

    public QualityPriceHotelDTO(String hotelId,
                                String hotelName,
                                String cityName,
                                String hotelRating,
                                int stars,
                                int facilitiesCount,
                                double averagePrice,
                                double qualityPriceRatio,
                                List<String> facilities) {
        this.hotelId = hotelId;
        this.hotelName = hotelName;
        this.cityName = cityName;
        this.hotelRating = hotelRating;
        this.stars = stars;
        this.facilitiesCount = facilitiesCount;
        this.averagePrice = averagePrice;
        this.qualityPriceRatio = qualityPriceRatio;
        this.facilities = facilities;
    }

    public String getHotelId() { return hotelId; }
    public String getHotelName() { return hotelName; }
    public String getCityName() { return cityName; }
    public String getHotelRating() { return hotelRating; }
    public int getStars() { return stars; }
    public int getFacilitiesCount() { return facilitiesCount; }
    public double getAveragePrice() { return averagePrice; }
    public double getQualityPriceRatio() { return qualityPriceRatio; }
    public List<String> getFacilities() { return facilities; }
}