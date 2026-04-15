package it.unipi.Voyager.dto;

public class TravellerConfigRequest {

    private String email;
    private String country;
    private String gender;
    private int age;

    // Preferences
    private String budget;
    private String season;
    private String travelType;

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
    public String getTravelType() { return travelType; }
    public void setTravelType(String travelType) { this.travelType = travelType; }
}