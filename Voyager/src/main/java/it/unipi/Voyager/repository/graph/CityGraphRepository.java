package it.unipi.Voyager.repository.graph;

import it.unipi.Voyager.dto.CitySimilarityDTO;
import it.unipi.Voyager.model.graph.CityNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface CityGraphRepository extends Neo4jRepository<CityNode, String> {

    @Query("MATCH (c1:City {cityName: $cityName})-[:IN_CITY]-(a1:Attraction) " +
            "WITH c1, a1.category AS type, COUNT(*) AS count1 " +
            "WITH c1, COLLECT({type: type, count: count1}) AS vec1 " +
            "MATCH (c2:City) WHERE c1 <> c2 " +
            "MATCH (c2)-[:IN_CITY]-(a2:Attraction) " +
            "WITH c1, c2, vec1, a2.category AS type, COUNT(*) AS count2 " +
            "WITH c1, c2, vec1, COLLECT({type: type, count: count2}) AS vec2 " +
            "WITH c1, c2, " +
            "REDUCE(dot = 0, v IN vec1 | dot + v.count * COALESCE([x IN vec2 WHERE x.type = v.type][0].count, 0)) AS dotProduct, " +
            "SQRT(REDUCE(n = 0, v IN vec1 | n + v.count * v.count)) AS norm1, " +
            "SQRT(REDUCE(n = 0, v IN vec2 | n + v.count * v.count)) AS norm2, " +
            "REDUCE(n = 0, v IN vec1 | n + v.count) AS totalC1, " +
            "REDUCE(n = 0, v IN vec2 | n + v.count) AS totalC2 " +
            "WITH c1, c2, " +
            "(dotProduct / (norm1 * norm2)) AS cosine, " +
            "1.0 / (1.0 + abs(totalC1 - totalC2)) AS sizePenalty " +
            "WITH c1, c2, (cosine * sizePenalty) AS attractionScore, " +
            "(CASE WHEN c1.category = c2.category THEN 1.0 ELSE 0.0 END * 0.30 + " +
            " CASE WHEN c1.bestTimeToVisit = c2.bestTimeToVisit THEN 1.0 ELSE 0.0 END * 0.25 + " +
            " CASE WHEN c1.costOfLiving = c2.costOfLiving THEN 1.0 ELSE 0.0 END * 0.05) AS metaScore " +
            "RETURN c2.cityName AS city, " +
            "(attractionScore * 0.60 + metaScore * 0.40) AS cosineSimilarity " +
            "ORDER BY cosineSimilarity DESC LIMIT 5")
    List<CitySimilarityDTO> findSimilarCities(String cityName);
}
