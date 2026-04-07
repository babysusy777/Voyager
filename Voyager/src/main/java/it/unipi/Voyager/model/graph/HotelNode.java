package it.unipi.Voyager.model.graph;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import java.util.List;

@Node("Hotel")
public class HotelNode {

    @Id
    private String hotelId; // Deve corrispondere all'ID di MongoDB [cite: 169]

    private String hotelName;

    private int stars; // Rating utile per i filtri di budget [cite: 169]

    // Relazione pesata: Hotel --NEAR_TO {distance}--> Attraction [cite: 232, 354]
    @Relationship(type = "NEAR_TO", direction = Relationship.Direction.OUTGOING)
    private List<HotelNearRel> nearbyAttractions;

    // Relazione semplice: Hotel --LOCATED_IN--> City [cite: 233, 352]
    @Relationship(type = "LOCATED_IN", direction = Relationship.Direction.OUTGOING)
    private CityNode city;

    // Getter e Setter
    public String getHotelId() { return hotelId; }
    public void setHotelId(String hotelId) { this.hotelId = hotelId; }

    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }

    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }

    public List<HotelNearRel> getNearbyAttractions() { return nearbyAttractions; }
    public void setNearbyAttractions(List<HotelNearRel> nearbyAttractions) { this.nearbyAttractions = nearbyAttractions; }

    public CityNode getCity() { return city; }
    public void setCity(CityNode city) { this.city = city; }
}