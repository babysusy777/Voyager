package it.unipi.Voyager.dto;

public class SeasonalConcentrationDTO {
    private String hotelName;
    private String peakSeason;
    private double concentrationRatio;
    private String riskLabel;

    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }

    public String getPeakSeason() { return peakSeason; }
    public void setPeakSeason(String peakSeason) { this.peakSeason = peakSeason; }

    public double getConcentrationRatio() { return concentrationRatio; }
    public void setConcentrationRatio(double concentrationRatio) { this.concentrationRatio = concentrationRatio; }

    public String getRiskLabel() { return riskLabel; }
    public void setRiskLabel(String riskLabel) { this.riskLabel = riskLabel; }
}