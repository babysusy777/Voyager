package it.unipi.Voyager.dto;

public class TravellerSegmentDTO {
    private int uniqueCities;
    private double avgStars;
    private double ratingStd;
    private double repeatRatio;
    private String segment;

    // Getters & Setters
    public int getUniqueCities() { return uniqueCities; }
    public void setUniqueCities(int uniqueCities) { this.uniqueCities = uniqueCities; }

    public double getAvgStars() { return avgStars; }
    public void setAvgStars(double avgStars) { this.avgStars = avgStars; }

    public double getRatingStd() { return ratingStd; }
    public void setRatingStd(double ratingStd) { this.ratingStd = ratingStd; }

    public double getRepeatRatio() { return repeatRatio; }
    public void setRepeatRatio(double repeatRatio) { this.repeatRatio = repeatRatio; }

    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }
}