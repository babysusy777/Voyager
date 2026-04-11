package it.unipi.Voyager.dto;

public class RecommendationDTO {
    private String cityName;
    private String hotelName;
    private Double finalScore;

    public RecommendationDTO(String cityName, String hotelName, Double finalScore) {
        this.cityName = cityName;
        this.hotelName = hotelName;
        this.finalScore = finalScore;
    }

    // Getter e Setter
}