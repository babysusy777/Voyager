package it.unipi.Voyager.dto;

import org.springframework.data.mongodb.core.mapping.Field;


public class HotelConcentrationDTO {

    @Field("cityName")
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