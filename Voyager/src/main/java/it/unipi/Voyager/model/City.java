package it.unipi.Voyager.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;
import java.util.Map;

@Document(collection = "cities")
public class City {

    @Id
    private ObjectId id;

    private String name;
    private String country;

    @Field("country_code")
    private String countryCode;

    private String description;

    @Field("avg_budget")
    private String avgBudget; // "low" | "medium" | "high"

    @Field("safety_index")
    private double safetyIndex;

    private Seasonality seasonality;

    @Field("city_index")
    private CityIndex cityIndex;

    private List<HotelSummary> hotels;

    @Field("top_attractions")
    private List<AttractionSummary> topAttractions;

    public static class Seasonality {
        private int spring;
        private int summer;
        private int autumn;
        private int winter;

        @Field("peak_season")
        private String peakSeason;

        @Field("concentration_ratio")
        private double concentrationRatio;

        // Getters & Setters
        public int getSpring() { return spring; }
        public void setSpring(int spring) { this.spring = spring; }

        public int getSummer() { return summer; }
        public void setSummer(int summer) { this.summer = summer; }

        public int getAutumn() { return autumn; }
        public void setAutumn(int autumn) { this.autumn = autumn; }

        public int getWinter() { return winter; }
        public void setWinter(int winter) { this.winter = winter; }

        public String getPeakSeason() { return peakSeason; }
        public void setPeakSeason(String peakSeason) { this.peakSeason = peakSeason; }

        public double getConcentrationRatio() { return concentrationRatio; }
        public void setConcentrationRatio(double concentrationRatio) { this.concentrationRatio = concentrationRatio; }
    }

    public static class CityIndex {
        @Field("total_visits")
        private int totalVisits;

        @Field("hotel_count")
        private int hotelCount;

        @Field("demand_ratio")
        private double demandRatio;

        // Getters & Setters
        public int getTotalVisits() { return totalVisits; }
        public void setTotalVisits(int totalVisits) { this.totalVisits = totalVisits; }

        public int getHotelCount() { return hotelCount; }
        public void setHotelCount(int hotelCount) { this.hotelCount = hotelCount; }

        public double getDemandRatio() { return demandRatio; }
        public void setDemandRatio(double demandRatio) { this.demandRatio = demandRatio; }
    }

    public static class HotelSummary {
        @Field("hotel_id")
        private String hotelId;

        @Field("hotel_name")
        private String hotelName;

        private int stars;

        @Field("avg_price")
        private double avgPrice;

        @Field("avg_rating")
        private double avgRating;

        // Getters & Setters
        public String getHotelId() { return hotelId; }
        public void setHotelId(String hotelId) { this.hotelId = hotelId; }

        public String getHotelName() { return hotelName; }
        public void setHotelName(String hotelName) { this.hotelName = hotelName; }

        public int getStars() { return stars; }
        public void setStars(int stars) { this.stars = stars; }

        public double getAvgPrice() { return avgPrice; }
        public void setAvgPrice(double avgPrice) { this.avgPrice = avgPrice; }

        public double getAvgRating() { return avgRating; }
        public void setAvgRating(double avgRating) { this.avgRating = avgRating; }
    }

    public static class AttractionSummary {
        @Field("attraction_id")
        private String attractionId;

        private String name;
        private String type;

        @Field("centrality_score")
        private double centralityScore;

        // Getters & Setters
        public String getAttractionId() { return attractionId; }
        public void setAttractionId(String attractionId) { this.attractionId = attractionId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public double getCentralityScore() { return centralityScore; }
        public void setCentralityScore(double centralityScore) { this.centralityScore = centralityScore; }
    }

    // ─── Getters & Setters (City) ─────────────────────────────────

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAvgBudget() { return avgBudget; }
    public void setAvgBudget(String avgBudget) { this.avgBudget = avgBudget; }

    public double getSafetyIndex() { return safetyIndex; }
    public void setSafetyIndex(double safetyIndex) { this.safetyIndex = safetyIndex; }

    public Seasonality getSeasonality() { return seasonality; }
    public void setSeasonality(Seasonality seasonality) { this.seasonality = seasonality; }

    public CityIndex getCityIndex() { return cityIndex; }
    public void setCityIndex(CityIndex cityIndex) { this.cityIndex = cityIndex; }

    public List<HotelSummary> getHotels() { return hotels; }
    public void setHotels(List<HotelSummary> hotels) { this.hotels = hotels; }

    public List<AttractionSummary> getTopAttractions() { return topAttractions; }
    public void setTopAttractions(List<AttractionSummary> topAttractions) { this.topAttractions = topAttractions; }
}