package it.unipi.Voyager.repository.graph;

import it.unipi.Voyager.model.graph.CityNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface CityGraphRepository extends Neo4jRepository<CityNode, String> {

    @Query("MATCH (c1:City {name: $cityName})-[:IN_CITY]-(a1:Attraction) " +
            "WITH c1, a1.type AS type, COUNT(*) AS count1 " +
            "WITH c1, COLLECT({type: type, count: count1}) AS vec1 " +
            "MATCH (c2:City) WHERE c1 <> c2 " +
            "MATCH (c2)-[:IN_CITY]-(a2:Attraction) " +
            "WITH c1, c2, vec1, a2.type AS type, COUNT(*) AS count2 " +
            "WITH c1, c2, vec1, COLLECT({type: type, count: count2}) AS vec2 " +
            "WITH c2, vec1, vec2, " +
            "REDUCE(dot = 0, v IN vec1 | dot + v.count * COALESCE([x IN vec2 WHERE x.type = v.type][0].count, 0)) AS dotProduct, " +
            "SQRT(REDUCE(n = 0, v IN vec1 | n + v.count * v.count)) AS norm1, " +
            "SQRT(REDUCE(n = 0, v IN vec2 | n + v.count * v.count)) AS norm2 " +
            "RETURN c2.name AS city, (dotProduct / (norm1 * norm2)) AS cosineSimilarity " +
            "ORDER BY cosineSimilarity DESC LIMIT 5")
    List<Map<String, Object>> findSimilarCities(String cityName);
}
