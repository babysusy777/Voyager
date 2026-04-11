package it.unipi.Voyager.model.graph;

import it.unipi.Voyager.model.Traveller;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Node("Traveller")
public class TravellerNode {

    @Id
    private String userId;
    private String email;

    private int age;
    private String gender;

    private String userSegment;
    private String travelType;

    private Traveller.Preferences preferences;

    // Relazione: (Traveller)-[:MADE_TRIP]->(Trip)
    @Relationship(type = "MADE_TRIP", direction = Relationship.Direction.OUTGOING)
    private List<TripNode> trips;

    // Relazione: (Traveller)-[:SIMILAR_TO]-(Traveller)
    @Relationship(type = "SIMILAR_TO", direction = Relationship.Direction.OUTGOING)
    private List<TravellerNode> similarTravellers;


    // Getters e Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }


    public String getUserSegment() { return userSegment; }
    public void setUserSegment(String userSegment) { this.userSegment = userSegment; }

    public Traveller.Preferences getPreferences() { return preferences; }
    public void setPreferences(Traveller.Preferences preferences) { this.preferences = preferences; }

    public List<TripNode> getTrips() { return trips; }
    public void setTrips(List<TripNode> trips) { this.trips = trips; }

    public List<TravellerNode> getSimilarTravellers() { return similarTravellers; }
    public void setSimilarTravellers(List<TravellerNode> similarTravellers) { this.similarTravellers = similarTravellers; }
}