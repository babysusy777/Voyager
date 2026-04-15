package it.unipi.Voyager.dto;

public class RecommendationDTO {
    private String cityName;
    private String hotelName;
    private Double finalScore;

    public RecommendationDTO() {}

    public RecommendationDTO(String cityName, String hotelName, Double finalScore) {
        this.cityName = cityName;
        this.hotelName = hotelName;
        this.finalScore = finalScore;
    }
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }

    public Double getFinalScore() { return finalScore; }
    public void setFinalScore(Double finalScore) { this.finalScore = finalScore; }
}
