package it.unipi.Voyager.model.graph;

import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class HotelNearRel {

    @TargetNode
    private AttractionNode attraction; // Il nodo di destinazione dell'arco [cite: 169, 234]

    private Double distance; // La proprietà "distanza" salvata sull'arco [cite: 169, 354]

    // --- GETTER E SETTER ---

    public AttractionNode getAttraction() {
        return attraction;
    }

    public void setAttraction(AttractionNode attraction) {
        this.attraction = attraction;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }
}