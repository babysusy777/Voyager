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

    @Query("MATCH (t1:Traveller {id: $travellerId}), (t2:Traveller) " +
            "WHERE t1 <> t2 " +
            "WITH t1, t2, " +
            "    (CASE WHEN t1.preferences.season = t2.preferences.season THEN 1 ELSE 0 END + " +
            "     CASE WHEN t1.preferences.budget = t2.preferences.budget THEN 1 ELSE 0 END + " +
            "     CASE WHEN t1.travelType = t2.traveltype AND t1.travelType IS NOT NULL THEN 1 ELSE 0 END + " +
            "     size(apoc.coll.intersection(t1.preferences, t2.preferences))) AS profileScore " +
            "OPTIONAL MATCH (t1)-[:MADE]->(:Trip)-[:IN_CITY]->(c:City)<-[:IN_CITY]-(:Trip)<-[:MADE]-(t2) " +
            "WITH t1, t2, profileScore, count(DISTINCT c) AS cityScore " +
            "OPTIONAL MATCH (t1)-[:MADE]->(:Trip)-[:STAYED_AT]->(h:Hotel)<-[:STAYED_AT]-(:Trip)<-[:MADE]-(t2) " +
            "WITH t1, t2, (profileScore * 1 + cityScore * 2 + count(DISTINCT h) * 4) AS finalScore " +
            "WHERE finalScore > 2 " +
            "MERGE (t1)-[s:SIMILAR_TO]->(t2) " +
            "SET s.score = finalScore")
    void computeAndSaveSimilarity(String travellerId);

    // Query per ottenere i primi 10 simili
    @Query("MATCH (t1:Traveller {id: $travellerId})-[s:SIMILAR_TO]->(t2:Traveller) " +
            "RETURN t2 ORDER BY s.score DESC LIMIT 10")
    List<TravellerNode> findTopSimilarTravellers(String travellerId);

    // Returning Travelers

    @Query("MATCH (t:Traveller {email: $email})-[:MADE_TRIP]->(trip:Trip)-[:IN_CITY]->(c:City)" +
            "MATCH (a:Attraction)-[:IN_CITY]->(c)" +
            "OPTIONAL MATCH (h:Hotel)-[:NEAR_TO]->(a)" +
            "WITH c, a, t, COUNT(DISTINCT h) AS centralityScore" +
            "WITH c, t, COLLECT({attr: a, score: centralityScore}) AS attrList" +
            "WITH c, t, attrList, apoc.coll.percentile([x IN attrList | x.score], 0.75) AS threshold" +
            "UNWIND attrList AS entry" +
            "WITH c, t, entry.attr AS a, entry.score AS centrality, threshold" +
            "OPTIONAL MATCH (t)-[:MADE_TRIP]->(trip:Trip)-[:STAYED_AT]->(hotel:Hotel)-[:NEAR_TO]->(a)" +
            "WITH c, a, centrality, threshold," +
            "     CASE" +
            "        WHEN centrality >= threshold AND t IS NULL THEN 'CENTRAL_NEW'" +
            "        WHEN centrality < threshold THEN 'HIDDEN_GEM'" +
            "     END AS status" +
            "WHERE status IN ['CENTRAL_NEW', 'HIDDEN_GEM']" +
            "RETURN c.cityName AS cityName, " +
            "       a.name AS attractionName, " +
            "       toFloat(centrality) AS score, " +
            "       status AS classification" +
            "ORDER BY cityName, score DESC")
    List<AttractionDiscoveryDTO> findReturningTravelerTips(String email);

    // Recommendations

    @Query("// 1. Identifica i tipi di attrazioni preferiti dall'utente (basato sui viaggi passati)" +
            "MATCH (me:Traveller {email: $email})-[:MADE_TRIP]->(:Trip)-[:STAYED_AT]->(:Hotel)-[:NEAR_TO]->(a:Attraction)" +
            "WITH me, collect(DISTINCT a.category) AS myPreferredCategories" +
            "" +
            "// 2. Trova i viaggiatori simili (score > soglia)" +
            "MATCH (me)-[sim:SIMILAR_TO]->(other:Traveller)" +
            "WHERE sim.score > 2.0" +
            "" +
            "// 3. Trova le città già visitate da me (per escluderle)" +
            "OPTIONAL MATCH (me)-[:MADE_TRIP]->(:Trip)-[:IN_CITY]->(visitedCity:City)" +
            "WITH me, other, sim.score AS similarityScore, myPreferredCategories, collect(DISTINCT visitedCity) AS myCities" +
            "" +
            "// 4. Trova Hotel e Città visitati dai 'simili' ma non da me" +
            "MATCH (other)-[:MADE_TRIP]->(t:Trip)-[:STAYED_AT]->(h:Hotel)-[:LOCATED_IN]->(newCity:City)" +
            "WHERE NOT newCity IN myCities" +
            "\n" +
            "// 5. Calcola la pertinenza delle attrazioni (nella città e vicino all'hotel)" +
            "OPTIONAL MATCH (newCity)<-[:IN_CITY]-(cityAttr:Attraction)" +
            "WHERE cityAttr.category IN myPreferredCategories" +
            "WITH me, h, newCity, similarityScore, myPreferredCategories, count(DISTINCT cityAttr) AS cityMatchCount" +
            "\n" +
            "OPTIONAL MATCH (h)-[:NEAR_TO]->(hotelAttr:Attraction)" +
            "WHERE hotelAttr.category IN myPreferredCategories" +
            "WITH newCity, h, similarityScore, cityMatchCount, count(DISTINCT hotelAttr) AS hotelMatchCount" +
            "\n" +
            "// 6. Calcolo finale del punteggio pesato\n" +
            "// Formula: (Somiglianza * 2) + (Attrazioni Città * 1) + (Attrazioni Hotel * 2)" +
            "WITH newCity, h," +
            "     (similarityScore * 2) + (cityMatchCount * 1) + (hotelMatchCount * 2) AS finalScore" +
            "RETURN newCity.cityName AS cityName, h.hotelName AS hotelName, toFloat(finalScore) AS finalScore" +
            "ORDER BY finalScore DESC" +
            "LIMIT 10")
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
}
