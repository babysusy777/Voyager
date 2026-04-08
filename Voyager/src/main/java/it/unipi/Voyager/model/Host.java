package it.unipi.Voyager.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "hosts")
public class Host {

    @Id
    private ObjectId id;

    private String username;
    private String email;
    private String password;

    @Field("full_name")
    private String fullName;

    private UserRole role;

    private List<HotelReference> hotels; //partial embedding

    // ─── Inner class ──────────────────────────────────────────────

    // le statistiche sui visitatori dell'hotel stanno dentro il relativo hotel.
    public static class HotelReference {

        @Field("hotel_id")
        private String hotelId;

        @Field("hotel_name")
        private String hotelName;

        private String city;

        private int stars;

        // Getters & Setters
        public String getHotelId() { return hotelId; }
        public void setHotelId(String hotelId) { this.hotelId = hotelId; }

        public String getHotelName() { return hotelName; }
        public void setHotelName(String hotelName) { this.hotelName = hotelName; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public int getStars() { return stars; }
        public void setStars(int stars) { this.stars = stars; }
    }

    // ─── Getters & Setters (Host) ─────────────────────────────────

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public List<HotelReference> getHotels() { return hotels; }
    public void setHotels(List<HotelReference> hotels) { this.hotels = hotels; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
}