package it.unipi.Voyager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.unipi.Voyager.model.TravelType;

public class TravellerConfigRequest {

    private String email;
    private String country;
    private String gender;
    private int age;

    // Preferences
    private String budget;
    private String season;

    @Schema(allowableValues = {"RELAX", "ADVENTURE", "CULTURAL", "BUSINESS", "FAMILY", "NIGHTLIFE"})
    private TravelType travelType;

    // Getters & Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }

    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
    public TravelType getTravelType() { return travelType; }
    public void setTravelType(TravelType travelType) { this.travelType = travelType; }
}