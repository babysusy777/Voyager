package it.unipi.Voyager.model.graph;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("City")
public class CityNode {

    @Id
    private String cityName;

    // Getter e Setter
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

}