package it.unipi.Voyager.model.graph;

import org.bson.types.ObjectId;
import org.springframework.data.neo4j.core.schema.Relationship;

public class TripNode {
    private ObjectId tripID;

    private int ratingGiven;

    @Relationship(type = "STAYED_AT", direction = Relationship.Direction.OUTGOING)
    private HotelNode hotel;

    // Getter e Setter
    public int getRatingGiven() { return ratingGiven;}
    public void setRatingGiven(int ratingGiven) { this.ratingGiven = ratingGiven; }

    public HotelNode getHotel() { return hotel; }
    public void setHotel(HotelNode hotel) { this.hotel = hotel; }
}
