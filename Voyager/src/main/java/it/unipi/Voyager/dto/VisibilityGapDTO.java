package it.unipi.Voyager.dto;

public class VisibilityGapDTO {
    private String hotelName;
    private String city;
    private String category;
    private int totalVisits;
    private double averagePeerVisits;
    private double gap;

    public VisibilityGapDTO() {}

    public VisibilityGapDTO(String hotelName, String city, String category, int actualVisits, double averagePeerVisits, double gap) {
        this.hotelName = hotelName;
        this.city = city;
        this.category = category;
        this.totalVisits = totalVisits;
        this.averagePeerVisits = averagePeerVisits;
        this.gap = gap;
    }

    // Getters e Setters
    public String getHotelName() { return hotelName; }
    public String getCity() { return city; }
    public String getCategory() { return category; }
    public int getTotalVisits() { return totalVisits; }
    public double getAveragePeerVisits() { return averagePeerVisits; }
    public double getGap() { return gap; }
    // ... setter omessi per brevità
}