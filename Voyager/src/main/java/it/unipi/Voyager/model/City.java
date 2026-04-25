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

    @Field("city_index")
    private CityIndex cityIndex;

    @Field("top_value_hotels")
    private List<HotelSummary> topValueHotels;
    @Field("other_hotel_ids")
    private List<String> otherHotelIds;

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

    public CityIndex getCityIndex() { return cityIndex; }
    public void setCityIndex(CityIndex cityIndex) { this.cityIndex = cityIndex; }

    public List<HotelSummary> getTopValueHotels() { return topValueHotels; }
    public void setTopValueHotels(List<HotelSummary> topValueHotels) { this.topValueHotels = topValueHotels; }

    public List<String> getOtherHotelIds() { return otherHotelIds; }
    public void setOtherHotelIds(List<String> otherHotelIds) { this.otherHotelIds = otherHotelIds; }

}