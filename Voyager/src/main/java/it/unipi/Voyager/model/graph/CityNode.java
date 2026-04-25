package it.unipi.Voyager.model.graph;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("City")
public class CityNode {

    @Id
    private String cityName;

    private String costOfLiving;
    private String category;
    private String bestTimeToVisit;

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getCostOfLiving() { return costOfLiving; }
    public void setCostOfLiving(String costOfLiving) { this.costOfLiving = costOfLiving; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBestTimeToVisit() { return bestTimeToVisit; }
    public void setBestTimeToVisit(String bestTimeToVisit) { this.bestTimeToVisit = bestTimeToVisit; }
}