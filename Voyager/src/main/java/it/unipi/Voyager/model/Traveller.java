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

    private String name;
    private String gender;
    private String email;
    private int age;
    private String country;

    private Preferences preferences;

    @Field("past_trips")
    private List<PastTrip> pastTrips;


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


    public static class PastTrip {

        @Field("trip_name")
        private String tripName;

        private String city;

        @Field("hotel_id")
        private String hotelId;

        private String season;
        private String date;

        @Field("rating_given")
        private int ratingGiven;

        // Getters & Setters
        public String getTripName() { return tripName; }
        public void setTripName(String tripName) { this.tripName = tripName; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getHotelId() { return hotelId; }
        public void setHotelId(String hotelId) { this.hotelId = hotelId; }

        public String getSeason() { return season; }
        public void setSeason(String season) { this.season = season; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public int getRatingGiven() { return ratingGiven; }
        public void setRatingGiven(int ratingGiven) { this.ratingGiven = ratingGiven; }
    }

    // ─── Getters & Setters (Traveller) ─────────────────────────

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

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

    public List<PastTrip> getPastTrips() { return pastTrips; }
    public void setPastTrips(List<PastTrip> pastTrips) { this.pastTrips = pastTrips; }
}