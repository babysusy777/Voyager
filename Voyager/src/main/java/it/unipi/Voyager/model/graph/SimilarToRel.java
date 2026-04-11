package it.unipi.Voyager.model.graph;

import it.unipi.Voyager.model.graph.TravellerNode;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class SimilarToRel {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private TravellerNode targetTraveller;

    private Double score; // Il SimilarityScore calcolato


    public SimilarToRel() {
    }

    public SimilarToRel(TravellerNode targetTraveller, Double score) {
        this.targetTraveller = targetTraveller;
        this.score = score;
    }

    // Getter e Setter

    public TravellerNode getTargetTraveller() {
        return targetTraveller;
    }

    public void setTargetTraveller(TravellerNode targetTraveller) {
        this.targetTraveller = targetTraveller;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}