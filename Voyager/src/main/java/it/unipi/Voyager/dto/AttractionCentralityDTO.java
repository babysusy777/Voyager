package it.unipi.Voyager.dto;

public class AttractionCentralityDTO {
    private String attractionName;
    private String category;
    private long hotelsSharing;
    private long coAttractions;
    private long centralityScore;

    public AttractionCentralityDTO(String attractionName, String category,
                                   long hotelsSharing, long coAttractions, long centralityScore) {
        this.attractionName  = attractionName;
        this.category        = category;
        this.hotelsSharing   = hotelsSharing;
        this.coAttractions   = coAttractions;
        this.centralityScore = centralityScore;
    }

    public String getAttractionName()  { return attractionName; }
    public String getCategory()        { return category; }
    public long getHotelsSharing()     { return hotelsSharing; }
    public long getCoAttractions()     { return coAttractions; }
    public long getCentralityScore()   { return centralityScore; }
}