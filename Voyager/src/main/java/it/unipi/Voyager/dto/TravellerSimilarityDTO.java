package it.unipi.Voyager.dto;

import it.unipi.Voyager.model.graph.TripNode;
import java.util.List;

public class TravellerSimilarityDTO {
    private String email;
    private int age;
    private String gender;
    private String userSegment;
    private String preferencesSeason;
    private List<TripSummaryDTO> trips;

    public TravellerSimilarityDTO(String email, int age, String gender,
                                  String userSegment, String preferencesSeason,
                                  List<TripSummaryDTO> trips) {
        this.email = email;
        this.age = age;
        this.gender = gender;
        this.userSegment = userSegment;
        this.preferencesSeason = preferencesSeason;
        this.trips = trips;
    }

    public String getEmail() { return email; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getUserSegment() { return userSegment; }
    public String getPreferencesSeason() { return preferencesSeason; }
    public List<TripSummaryDTO> getTrips() { return trips; }
}