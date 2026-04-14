package it.unipi.Voyager.model;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = "cities")
@CompoundIndex(name = "city_name_unique_idx", def = "{'cityName': 1}", unique = true)
public class City {


    @Field("cityName")
    private String name;

    @Field("cost_of_living")
    private String costOfLiving;

    @Field("safety")
    private String safety;

    @Field("category")
    private String category;

    @Field("best_time_to_visit")
    private String bestTimeToVisit;

    private Seasonality seasonality;

    @Field("top_value_hotels")
    private List<HotelSummary> topValueHotels;
    @Field("other_hotel_ids")
    private List<String> otherHotelIds;

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

        @Field("demand_ratio")
        private double demandVsSupplyRatio;

        // Getters & Setters

        public double getDemandRatio() { return demandVsSupplyRatio; }
        public void setDemandRatio(double demandVsSupplyRatio) { this.demandVsSupplyRatio = demandVsSupplyRatio; }
    }

    public static class HotelSummary { // Partial Embedding

        @Field("hotel_name")
        private String hotelName;

        @Field("stars")
        private String stars;

        @Field("avg_price")
        private double avgPrice;


        // Getters & Setters
        public String getHotelName() { return hotelName; }
        public void setHotelName(String hotelName) { this.hotelName = hotelName; }

        public String getHotelStars() { return stars; }
        public void setHotelStars(String stars) { this.stars = stars; }

        public double getAvgPrice() { return avgPrice; }
        public void setAvgPrice(double avgPrice) { this.avgPrice = avgPrice; }


    }

    public static class AttractionSummary {
        //@Field("attraction_id")
        //private String attractionId;

        private String name;
        @Field("category")
        private String type;

        @Field("centrality_score")
        private double centralityScore;

        // Getters & Setters
        //public String getAttractionId() { return attractionId; }
        //public void setAttractionId(String attractionId) { this.attractionId = attractionId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public double getCentralityScore() { return centralityScore; }
        public void setCentralityScore(double centralityScore) { this.centralityScore = centralityScore; }
    }

    // ─── Getters & Setters (City) ─────────────────────────────────


    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCostOfLiving() { return costOfLiving; }
    public void setCostOfLiving(String costOfLiving) { this.costOfLiving = costOfLiving; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBestTimeToVisit() { return bestTimeToVisit; }
    public void setBestTimeToVisit(String bestTimeToVisit) { this.bestTimeToVisit = bestTimeToVisit; }

    public String getSafety() { return safety; }
    public void setSafety(String safetyIndex) { this.safety = safety; }

    public Seasonality getSeasonality() { return seasonality; }
    public void setSeasonality(Seasonality seasonality) { this.seasonality = seasonality; }


    public List<HotelSummary> getTopValueHotels() { return topValueHotels; }
    public void setTopValueHotels(List<HotelSummary> topValueHotels) { this.topValueHotels = topValueHotels; }

    public List<String> getOtherHotelIds() { return otherHotelIds; }
    public void setOtherHotelIds(List<String> otherHotelIds) { this.otherHotelIds = otherHotelIds; }

    public List<AttractionSummary> getTopAttractions() { return topAttractions; }
    public void setTopAttractions(List<AttractionSummary> topAttractions) { this.topAttractions = topAttractions; }

}