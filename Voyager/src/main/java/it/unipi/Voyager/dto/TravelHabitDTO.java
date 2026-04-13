package it.unipi.Voyager.dto;

public class TravelHabitDTO {
    private String mostFrequentSeason;
    private int totalTrips;
    private Double avgRating;
    private int countCities;

    // Getter e Setter sono fondamentali per Spring
    public String getMostFrequentSeason() { return mostFrequentSeason; }
    public void setMostFrequentSeason(String mostFrequentSeason) { this.mostFrequentSeason = mostFrequentSeason; }
    public int getTotalTrips() { return totalTrips; }
    public void setTotalTrips(int totalTrips) { this.totalTrips = totalTrips; }
    public Double getAvgRating() { return avgRating; }
    public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }
    public int getCountCities() { return countCities; }
    public void setCountCities(int countCities) { this.countCities = countCities; }
}