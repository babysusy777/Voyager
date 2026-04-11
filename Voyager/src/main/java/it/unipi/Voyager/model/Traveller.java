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
    private UserRole role;

    @Field("past_trips")
    private List<Trip> trips;

    @Field("travel_type")
    private String travelType;


    public static class Preferences {
        private String budget;

        //@Field("travel_type")
        //private String travelType;

        private String season;

        // Getters & Setters
        public String getBudget() { return budget; }
        public void setBudget(String budget) { this.budget = budget; }

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
        private List<HotelSummary> hotel; // solo nome, no embedding o linking

        private String season;
        private String date;

        @Field("rating_given")
        private int ratingGiven;

        public static class HotelSummary {
            @Field("hotel_name")
            private String hotelName;

            private String stars;

            public HotelSummary(String name, String stars) {
                this.hotelName = name;
                this.stars = stars;
            }
            public String getHotelName() { return hotelName; }
            public void setHotelName(String hotelName) { this.hotelName = hotelName; }

            public String getHotelStars() { return stars; }
            public void setHotelStars(String stars) { this.stars = stars; }
        }

        // Getters & Setters
        public String getTripName() { return tripName; }
        public void setTripName(String tripName) { this.tripName = tripName; }

        public List<String> getCity() { return city; }
        public void setCity(List<String> city) { this.city = city; }

        public List<HotelSummary> getHotels() { return hotel; }
        public void setHotels(List<HotelSummary> hotel) { this.hotel = hotel; }

        public String getSeason() { return season; }
        public void setSeason(String season) { this.season = season; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public int getRatingGiven() { return ratingGiven; }
        public void setRatingGiven(int ratingGiven) { this.ratingGiven = ratingGiven; }
    }

    // ─── Getters & Setters (Traveler) ─────────────────────────

    public String getTravelType() { return travelType; }
    public void setTravelType(String travelType) { this.travelType = travelType; }
    
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

    public String getUserSegment() { return userSegment; }
    public void setUserSegment(String userSegment) { this.userSegment = userSegment; }

    public String getBudget() { return budget; }
    public void setBudget(String budget) { this.budget = budget; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public void setRole(UserRole role) {this.role = role;}
    public UserRole getRole() { return role;}
}