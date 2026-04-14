package it.unipi.Voyager.dto;

/**
 * Data Transfer Object per l'analisi Demand vs Supply della città.
 * Ottimizzato per riflettere l'uso del Compound Index (City + Rating).
 */
public class CityIndexDTO {
    private String cityName;
    private int hotelCount;
    private int totalCityVisits;
    private Double demandRatio;
    private String status;

    // --- GETTER E SETTER ---

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }
    
    public int getHotelCount() { return hotelCount; }
    public void setHotelCount(int hotelCount) { this.hotelCount = hotelCount; }

    public int getTotalCityVisits() { return totalCityVisits; }
    public void setTotalCityVisits(int totalCityVisits) { this.totalCityVisits = totalCityVisits; }

    public Double getDemandRatio() { return demandRatio; }
    public void setDemandRatio(Double demandRatio) { this.demandRatio = demandRatio; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}