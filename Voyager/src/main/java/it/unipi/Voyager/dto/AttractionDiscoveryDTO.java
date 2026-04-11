package it.unipi.Voyager.dto;

public class AttractionDiscoveryDTO {
    private String cityName;
    private String attractionName;
    private Double score;
    private String classification; // 'CENTRAL_NEW' o 'HIDDEN_GEM'

    // Costruttore per Spring Data Neo4j
    public AttractionDiscoveryDTO(String cityName, String attractionName, Double score, String classification) {
        this.cityName = cityName;
        this.attractionName = attractionName;
        this.score = score;
        this.classification = classification;
    }

    // Getter e Setter
    public String getCityName() { return cityName; }
    public String getAttractionName() { return attractionName; }
    public Double getScore() { return score; }
    public String getClassification() { return classification; }
}