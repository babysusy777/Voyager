package it.unipi.Voyager.dto;



/**
 * Data Transfer Object per l'analisi Demand vs Supply della città.
 * Riflette i campi calcolati dalla pipeline di aggregazione sulla collezione hotels.
 */
public class CityIndexDTO {
    private String cityName;      // Nome della città analizzata
    private int hotelCount;       // Numero di hotel (Offerta)
    private int totalCityVisits;  // Somma dei totalVisits di tutti gli hotel (Domanda) [cite: 166]
    private Double demandRatio;   // Rapporto visite/hotel (Pressure Index)
    private String status;        // Stato del mercato: UNDERSUPPLIED, OVERSUPPLIED, BALANCED [cite: 166]

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