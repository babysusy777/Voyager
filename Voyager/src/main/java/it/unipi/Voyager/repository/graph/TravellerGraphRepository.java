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

    //recommendation based on past trips and preferences
    @Query("""
        MATCH (me:Traveller {email: $email})
            
        // Deriva categorie preferite dai trip passati
        MATCH (me)-[:MADE_TRIP]->(:Trip)-[:STAYED_AT]->(:Hotel)-[:NEAR_TO]->(a:Attraction)
        WITH me, collect(DISTINCT a.category) AS myPreferredCategories

        // Città già visitate dal target
        OPTIONAL MATCH (me)-[:MADE_TRIP]->(:Trip)-[:STAYED_AT]->(:Hotel)-[:LOCATED_IN]->(visitedCity:City)
        WITH me, myPreferredCategories, collect(DISTINCT visitedCity) AS myCities

        // Candidate: città non ancora visitate
        MATCH (h:Hotel)-[:LOCATED_IN]->(newCity:City)
        WHERE NOT newCity IN myCities

        // Match attrazioni città con preferenze
        OPTIONAL MATCH (newCity)<-[:IN_CITY]-(cityAttr:Attraction)
        WITH me, h, newCity, myPreferredCategories, count(DISTINCT cityAttr) AS totalCityAttr

        OPTIONAL MATCH (newCity)<-[:IN_CITY]-(matchAttr:Attraction)
        WHERE matchAttr.category IN myPreferredCategories
        WITH me, h, newCity, myPreferredCategories, totalCityAttr, count(DISTINCT matchAttr) AS cityMatchCount

        // Match attrazioni vicine all'hotel con preferenze
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

    //calcola la similarity e la recommendation based on similar travellers
    @Query("""
            MATCH (me:Traveller {email: $email})
            MATCH (other:Traveller) WHERE me <> other
            WITH me, other,
                (CASE WHEN me.preferencesSeason = other.preferencesSeason THEN 1 ELSE 0 END +
                 CASE WHEN me.preferencesBudget = other.preferencesBudget THEN 1 ELSE 0 END +
                 CASE WHEN me.travelType = other.travelType AND me.travelType IS NOT NULL THEN 1 ELSE 0 END +
                 CASE WHEN abs(me.age - other.age) <= 5 THEN 0.5 ELSE 0 END) AS profileScore
            
            OPTIONAL MATCH (me)-[:MADE_TRIP]->(:Trip)-[:STAYED_AT]->(h:Hotel)<-[:STAYED_AT]-(:Trip)<-[:MADE_TRIP]-(other)
            WITH me, other, profileScore, count(DISTINCT h) AS hotelScore
            WITH me, other, (profileScore * 1 + hotelScore * 4) AS similarityScore
            WHERE similarityScore > 2
            ORDER BY similarityScore DESC LIMIT 5
            
            // Città già visitate dal target
            OPTIONAL MATCH (me)-[:MADE_TRIP]->(:Trip)-[:STAYED_AT]->(:Hotel)-[:LOCATED_IN]->(visitedCity:City)
            WITH me, other, similarityScore, collect(DISTINCT visitedCity) AS myCities
            
            // Città e hotel visitati dai simili, non ancora visitati dal target
            MATCH (other)-[:MADE_TRIP]->(trip:Trip)-[:STAYED_AT]->(h:Hotel)-[:LOCATED_IN]->(c:City)
            WHERE NOT c IN myCities
            
            WITH c, h, trip, similarityScore,
                 count(DISTINCT other) AS peerCount,
                 avg(toFloat(trip.ratingGiven)) AS avgPeerRating,
                 sum(similarityScore) AS similarityMass
            
            WITH c, h,
                 peerCount,
                 avgPeerRating,
                 similarityMass,
                 (peerCount * avgPeerRating * similarityMass) AS finalScore
            WHERE finalScore > 0
            
            ORDER BY c.cityName, finalScore DESC
            WITH c, collect({hotelName: h.hotelName, finalScore: finalScore})[0] AS best
            
            RETURN c.cityName AS cityName, best.hotelName AS hotelName, toFloat(best.finalScore) AS finalScore
            ORDER BY best.finalScore DESC
            LIMIT 10
    """)
    List<RecommendationDTO> getSimilarTravellersRecommendation(String email);

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
