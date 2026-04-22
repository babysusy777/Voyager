package it.unipi.Voyager.repository.graph;

import it.unipi.Voyager.dto.AttractionDiscoveryDTO;
import it.unipi.Voyager.dto.RecommendationDTO;
import it.unipi.Voyager.model.graph.TravellerNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravellerGraphRepository extends Neo4jRepository<TravellerNode, String> {

    //Travelers who have a similar profile in terms of personal information,
    //preferences, preferred season and budget range,
    //and that visited the same cities (medium weight), or same hotels (heavy weight)

    @Query("""
        MATCH (t1:Traveller {email: $email})
        MATCH (t2:Traveller) WHERE t1 <> t2
        WITH t1, t2,
            (CASE WHEN t1.preferencesSeason = t2.preferencesSeason THEN 1 ELSE 0 END +
             CASE WHEN t1.preferencesBudget = t2.preferencesBudget THEN 1 ELSE 0 END +
             CASE WHEN t1.travelType = t2.travelType AND t1.travelType IS NOT NULL THEN 1 ELSE 0 END +
             // Calcolo differenza età: se <= 5 aggiunge 0.5
             CASE WHEN abs(t1.age - t2.age) <= 5 THEN 0.5 ELSE 0 END) AS profileScore
        
        OPTIONAL MATCH (t1)-[:MADE_TRIP]->(:Trip)-[:STAYED_AT]->(h:Hotel)<-[:STAYED_AT]-(:Trip)<-[:MADE_TRIP]-(t2)
        
        WITH t1, t2, profileScore, count(DISTINCT h) AS hotelScore
        WITH t1, t2, (profileScore * 1 + hotelScore * 4) AS finalScore
        WHERE finalScore > 2
        
        MERGE (t1)-[s:SIMILAR_TO]->(t2)
        SET s.score = finalScore
    """)
    void computeAndSaveSimilarity(String email);

    // Query per ottenere i primi 10 simili
    @Query("MATCH (t1:Traveller {email: $email})-[s:SIMILAR_TO]->(t2:Traveller) " +
            "OPTIONAL MATCH (t2)-[:MADE_TRIP]->(trip:Trip) " +
            "WITH t2, s, collect(trip) AS trips " +
            "RETURN t2, trips ORDER BY s.score DESC LIMIT 10")
    List<TravellerNode> findTopSimilarTravellers(String email);

    // Returning Travelers
    @Query("""
        MATCH (t:Traveller {email: $email})-[:MADE_TRIP]->(trip:Trip)-[:STAYED_AT]->(h:Hotel)-[:LOCATED_IN]->(c:City)
        MATCH (a:Attraction)-[:IN_CITY]->(c)
        OPTIONAL MATCH (h2:Hotel)-[:NEAR_TO]->(a)
        WITH c, a, t, COUNT(DISTINCT h2) AS centralityScore
        ORDER BY centralityScore DESC
        WITH c, t, COLLECT({attr: a, score: centralityScore}) AS attrList
        WITH c, t, attrList,
             attrList[toInteger(size(attrList) * 0.25)].score AS threshold
        UNWIND attrList AS entry
        WITH c, t, entry.attr AS a, entry.score AS centrality, threshold
        OPTIONAL MATCH (t)-[:MADE_TRIP]->(:Trip)-[:STAYED_AT]->(hotel:Hotel)-[:NEAR_TO]->(a)
        WITH c, a, centrality, threshold, hotel,
             CASE
                WHEN centrality >= threshold AND hotel IS NULL THEN 'CENTRAL_NEW'
                WHEN centrality < threshold THEN 'HIDDEN_GEM'
             END AS status
        WHERE status IN ['CENTRAL_NEW', 'HIDDEN_GEM']
        RETURN c.cityName AS cityName,
               a.name AS attractionName,
               toFloat(centrality) AS score,
               status AS classification
        ORDER BY cityName, score DESC
    """)
    List<AttractionDiscoveryDTO> findReturningTravelerTips(String email);

    // Recommendations
    @Query("""
        MATCH (me:Traveller {email: $email})-[:MADE_TRIP]->(:Trip)-[:STAYED_AT]->(:Hotel)-[:NEAR_TO]->(a:Attraction)
        WITH me, collect(DISTINCT a.category) AS myPreferredCategories
    
        OPTIONAL MATCH (me)-[:MADE_TRIP]->(:Trip)-[:IN_CITY]->(visitedCity:City)
        WITH me, myPreferredCategories, collect(DISTINCT visitedCity) AS myCities
    
        MATCH (h:Hotel)-[:LOCATED_IN]->(newCity:City)
        WHERE NOT newCity IN myCities
    
        OPTIONAL MATCH (newCity)<-[:IN_CITY]-(cityAttr:Attraction)
        WITH me, h, newCity, myPreferredCategories, count(DISTINCT cityAttr) AS totalCityAttr
    
        OPTIONAL MATCH (newCity)<-[:IN_CITY]-(matchAttr:Attraction)
        WHERE matchAttr.category IN myPreferredCategories
        WITH me, h, newCity, myPreferredCategories, totalCityAttr, count(DISTINCT matchAttr) AS cityMatchCount
    
        OPTIONAL MATCH (h)-[:NEAR_TO]->(hotelAttr:Attraction)
        WHERE hotelAttr.category IN myPreferredCategories
        OPTIONAL MATCH (h)-[:NEAR_TO]->(anyAttr:Attraction)
        WITH newCity, h, cityMatchCount, totalCityAttr,
             count(DISTINCT hotelAttr) AS hotelMatchCount,
             count(DISTINCT anyAttr) AS totalHotelAttr
    
        WITH newCity, h,
             (toFloat(cityMatchCount) / (totalCityAttr + 1)) * 10 +
             (toFloat(hotelMatchCount) / (totalHotelAttr + 1)) * 20 AS finalScore
        WHERE finalScore > 0
    
        ORDER BY newCity.cityName, finalScore DESC
        WITH newCity, collect({hotelName: h.hotelName, finalScore: finalScore})[0] AS best
    
        RETURN newCity.cityName AS cityName, best.hotelName AS hotelName, toFloat(best.finalScore) AS finalScore
        ORDER BY best.finalScore DESC
        LIMIT 10
    """)
    List<RecommendationDTO> getPersonalizedRecommendations(String email);
    // Bulk — inizializzazione
    @Query("""
                MATCH (t:Traveller)
                OPTIONAL MATCH (t)-[:MADE_TRIP]->()-[:STAYED_AT]->(h:Hotel)-[:NEAR_TO]->(a:Attraction)
        WITH t, a.category AS category, COUNT(a) AS categoryCount
        ORDER BY categoryCount DESC
        WITH t, CASE
            WHEN COUNT(category) = 0 THEN null
            ELSE COLLECT(category)[0]
        END AS dominantCategory
        SET t.travelType = dominantCategory
    """)
    void computeAndStoreTravelTypeAll();

    // Singolo — dopo nuovo trip
    @Query("""
        MATCH (t:Traveller {email: $email})
        OPTIONAL MATCH (t)-[:MADE_TRIP]->()-[:STAYED_AT]->(h:Hotel)-[:NEAR_TO]->(a:Attraction)
        WITH t, a.category AS category, COUNT(a) AS categoryCount
        ORDER BY categoryCount DESC
        WITH t, CASE
            WHEN COUNT(category) = 0 THEN null
            ELSE COLLECT(category)[0]
        END AS dominantCategory
        SET t.travelType = dominantCategory
        RETURN t.travelType
    """)
    String computeAndStoreTravelType(String email);

    @Query("MATCH (t:Traveller {email: $email})-[r:MADE_TRIP]->(trip:Trip) " +
            "OPTIONAL MATCH (trip)-[s:STAYED_AT]->() " +
            "DELETE s, r, trip")
    void deleteTripsByEmail(String email);
}
