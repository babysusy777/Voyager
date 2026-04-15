package it.unipi.Voyager.dto;

public class TrendResponseDTO {
    private String userId;
    private String trend; 
    private String message;

    public TrendResponseDTO(String userId, String trend) {
        this.userId = userId;
        this.trend = trend;
    }

    // Getters e Setters
    public String getUserId() { return userId; }
    public String getTrend() { return trend; }
}