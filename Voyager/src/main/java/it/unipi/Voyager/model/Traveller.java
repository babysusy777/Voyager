package it.unipi.Voyager.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "travellers")
public class Traveller {

    @Id
    private ObjectId id;

    @Field("user_id")
    private String userId;

    @Field("name")
    private String fullName;
    private String gender;
    private String email;
    private int age;
    private String userSegment;
    private String budget;
    private String country;
    private String password;
    private Preferences preferences;

    @Field("past_trips")
    private List<Trip> trips;


    public static class Preferences {

        private String budget;

        @Field("travel_type")
        private String travelType;

        private String season;

        // Getters & Setters
        public String getBudget() { return budget; }
        public void setBudget(String budget) { this.budget = budget; }

        public String getTravelType() { return travelType; }
        public void setTravelType(String travelType) { this.travelType = travelType; }

        public String getSeason() { return season; }
        public void setSeason(String season) { this.season = season; }
    }


    public static class Trip {

        @Id
        private ObjectId id;

        @Field("trip_name")
        private String tripName;

        private List<String> city; // partial embedding + linking, ci serve solo il nome

        @Field("hotel_name")
        private List<String> hotelName; // solo nome, no embedding o linking

        private String season;
        private String date;

        @Field("rating_given")
        private int ratingGiven;

        // Getters & Setters
        public String getTripName() { return tripName; }
        public void setTripName(String tripName) { this.tripName = tripName; }

        public List<String> getCity() { return city; }
        public void setCity(List<String> city) { this.city = city; }

        public List<String> getHotelName() { return hotelName; }
        public void setHotelName(List<String> hotelName) { this.hotelName = hotelName; }

        public String getSeason() { return season; }
        public void setSeason(String season) { this.season = season; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public int getRatingGiven() { return ratingGiven; }
        public void setRatingGiven(int ratingGiven) { this.ratingGiven = ratingGiven; }
    }

    // ─── Getters & Setters (Traveler) ─────────────────────────

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String name) { this.fullName = name; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Preferences getPreferences() { return preferences; }
    public void setPreferences(Preferences preferences) { this.preferences = preferences; }

    public List<Trip> getTrips() { return trips; }
    public void setTrips(List<Trip> trips) { this.trips = trips; }

    public String getUser_segment() { return userSegment; }
    public void setUser_segment(String user_segment) { this.userSegment = user_segment; }

    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}