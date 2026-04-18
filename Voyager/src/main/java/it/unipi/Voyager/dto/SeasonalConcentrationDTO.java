package it.unipi.Voyager.dto;

import org.springframework.data.mongodb.core.mapping.Field;

public class SeasonalConcentrationDTO {
    private String HotelName;

    @Field("peakSeason")
    private String peakSeason;

    @Field("concentrationRatio")
    private double concentrationRatio;

    @Field("riskLabel")
    private String riskLabel;

    public SeasonalConcentrationDTO() {}

    public String getHotelName() { return HotelName; }
    public void setHotelName(String HotelName) { this.HotelName = HotelName; }

    public String getPeakSeason() { return peakSeason; }
    public void setPeakSeason(String peakSeason) { this.peakSeason = peakSeason; }

    public double getConcentrationRatio() { return concentrationRatio; }
    public void setConcentrationRatio(double concentrationRatio) { this.concentrationRatio = concentrationRatio; }

    public String getRiskLabel() { return riskLabel; }
    public void setRiskLabel(String riskLabel) { this.riskLabel = riskLabel; }
}