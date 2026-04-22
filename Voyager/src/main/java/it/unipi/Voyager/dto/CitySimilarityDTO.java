package it.unipi.Voyager.dto;

public class CitySimilarityDTO {
    private String city;
    private Double cosineSimilarity;

    public CitySimilarityDTO(String city, Double cosineSimilarity) {
        this.city = city;
        this.cosineSimilarity = cosineSimilarity;
    }

    public String getCity() { return city; }
    public Double getCosineSimilarity() { return cosineSimilarity; }
}
