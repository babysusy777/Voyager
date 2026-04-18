package it.unipi.Voyager.dto;

import org.springframework.data.mongodb.core.mapping.Field;

public class SeasonalConcentrationDTO {
    @Field("hotelName")
    private String hotelName;

    @Field("peakSeason")
    private String peakSeason;

    @Field("concentrationRatio")
    private double concentrationRatio;

    @Field("riskLabel")
    private String riskLabel;

    public SeasonalConcentrationDTO(String hotelName, String peakSeason, double concentrationRatio, String riskLabel) {
        this.hotelName = hotelName;
        this.peakSeason = peakSeason;
        this.concentrationRatio = concentrationRatio;
        this.riskLabel = riskLabel;
    }

    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }

    public String getPeakSeason() { return peakSeason; }
    public void setPeakSeason(String peakSeason) { this.peakSeason = peakSeason; }

    public double getConcentrationRatio() { return concentrationRatio; }
    public void setConcentrationRatio(double concentrationRatio) { this.concentrationRatio = concentrationRatio; }

    public String getRiskLabel() { return riskLabel; }
    public void setRiskLabel(String riskLabel) { this.riskLabel = riskLabel; }
}