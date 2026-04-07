package it.unipi.Voyager.model.graph;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Attraction")
public class AttractionNode {

    @Id
    private String name; // Identificativo univoco del nodo [cite: 169]

    private String category; // Tipologia (es. sport, cultural) [cite: 169]

    @Relationship(type = "IN_CITY", direction = Relationship.Direction.OUTGOING)
    private CityNode city;

    // Getter e Setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public CityNode getCity() { return city; }
    public void setCity(CityNode city) { this.city = city; }
}