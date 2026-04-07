package it.unipi.Voyager.model.graph;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import java.util.List;

@Node("Hotel")
public class HotelNode {

    private String hotelName;

    private int stars; // Rating utile per i filtri di budget

    // Relazione pesata: Hotel --NEAR_TO {distance}--> Attraction
    @Relationship(type = "NEAR_TO", direction = Relationship.Direction.OUTGOING)
    private List<HotelNearRel> nearbyAttractions;

    // Relazione semplice: Hotel --LOCATED_IN--> City
    @Relationship(type = "LOCATED_IN", direction = Relationship.Direction.OUTGOING)
    private CityNode city;

    // Getter e Setter

    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }

    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }

    public List<HotelNearRel> getNearbyAttractions() { return nearbyAttractions; }
    public void setNearbyAttractions(List<HotelNearRel> nearbyAttractions) { this.nearbyAttractions = nearbyAttractions; }

    public CityNode getCity() { return city; }
    public void setCity(CityNode city) { this.city = city; }
}