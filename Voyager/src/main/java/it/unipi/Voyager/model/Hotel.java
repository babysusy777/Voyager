package it.unipi.Voyager.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.List;
import java.util.Map;

@Document(collection = "hotels")
// Indice fondamentale per le query Host in cui c'è il confronto tra hotel della stessa città e categoria.
@CompoundIndex(name = "city_stars_idx", def = "{'cityName': 1, 'HotelRating': 1}")
public class Hotel {

    @Id
    private String id;

    @Field("HotelCode")
    private Long hotelCode;

    @Field("HotelName")
    private String hotelName;

    @Field("cityName")
    private String cityName;

    @Field("HotelRating")
    private String hotelRating; // es. "FourStar"

    @Field("Address")
    private String address;

    @Field("average_price_per_night")
    private Double averagePrice;

    @Field("HotelFacilities")
    private List<String> facilities; // Stringa o lista di servizi

    @Field("Description")
    private String description;


    /**
     * Statistiche precomputate per la Dashboard Host
     */
    private GuestStats guestStats;

    // --- GETTER E SETTER ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getHotelCode() { return hotelCode; }
    public void setHotelCode(Long hotelCode) { this.hotelCode = hotelCode; }

    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getHotelRating() { return hotelRating; }
    public void setHotelRating(String hotelRating) { this.hotelRating = hotelRating; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getAveragePrice() { return averagePrice; }
    public void setAveragePrice(Double averagePrice) { this.averagePrice = averagePrice; }

    public List<String> getFacilities() { return facilities; }
    public void setFacilities(List<String> facilities) { this.facilities = facilities; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public GuestStats getGuestStats() { return guestStats; }
    public void setGuestStats(GuestStats guestStats) { this.guestStats = guestStats; }

    // --- CLASSI INTERNE PER STATISTICHE (Writeback) ---

    public static class GuestStats {
        private int totalVisits;
        private SeasonStats seasonality;
        private SegmentDistribution segmentDistribution;
        @Field("city_category_avg_visits")
        private double cityCategoryAvgVisits;


        // Getter e Setter per GuestStats...
        public int getTotalVisits() { return totalVisits; }
        public void setTotalVisits(int totalVisits) { this.totalVisits = totalVisits; }
        public SeasonStats getSeasonality() { return seasonality; }
        public void setSeasonality(SeasonStats seasonality) { this.seasonality = seasonality; }
        public SegmentDistribution getSegmentDistribution() { return segmentDistribution; }
        public void setSegmentDistribution(SegmentDistribution segmentDistribution) { this.segmentDistribution = segmentDistribution; }
        public double getCityCategoryAvgVisits() { return cityCategoryAvgVisits; }
        public void setCityCategoryAvgVisits(double cityCategoryAvgVisits) {this.cityCategoryAvgVisits = cityCategoryAvgVisits;  }
    }

    public static class SeasonStats {
        private Map<String, Integer> counts;
        private String peakSeason;
        private Double concentrationRatio; // KPI: rischio stagionalità [cite: 159, 293]

        // Getter e Setter per SeasonStats...
        public Map<String, Integer> getCounts() { return counts; }
        public void setCounts(Map<String, Integer> counts) { this.counts = counts; }
        public String getPeakSeason() { return peakSeason; }
        public void setPeakSeason(String peakSeason) { this.peakSeason = peakSeason; }
        public Double getConcentrationRatio() { return concentrationRatio; }
        public void setConcentrationRatio(Double concentrationRatio) { this.concentrationRatio = concentrationRatio; }
    }

    public static class SegmentDistribution {
        private Map<String, Double> segments; // explorer, comfort-seeker, etc. [cite: 101, 262]
        private String dominantSegment;

        // Getter e Setter per SegmentDistribution...
        public Map<String, Double> getSegments() { return segments; }
        public void setSegments(Map<String, Double> segments) { this.segments = segments; }
        public String getDominantSegment() { return dominantSegment; }
        public void setDominantSegment(String dominantSegment) { this.dominantSegment = dominantSegment; }
    }
}