package it.unipi.Voyager.repository;

import it.unipi.Voyager.dto.AttractionCentralityDTO;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;
import it.unipi.Voyager.model.graph.AttractionNode;

import java.util.List;

@Repository
public interface AttractionRepository extends Neo4jRepository<AttractionNode, String> {

    @Query("""
        MATCH (h:Hotel)-[:LOCATED_IN]->(c:City {cityName: $cityName})
        MATCH (h)-[:NEAR_TO]->(a:Attraction)
        WITH a, COLLECT(DISTINCT h) AS hotels, COUNT(DISTINCT h) AS hotelsSharing
        UNWIND hotels AS h
        MATCH (h)-[:NEAR_TO]->(a2:Attraction)
        WHERE a2 <> a
        WITH a, hotelsSharing, COUNT(DISTINCT a2) AS coAttractions
        RETURN a.name         AS attractionName,
               a.category     AS category,
               hotelsSharing  AS hotelsSharing,
               coAttractions  AS coAttractions,
               hotelsSharing * coAttractions AS centralityScore
        ORDER BY centralityScore DESC
        LIMIT 10
    """)
    List<AttractionCentralityDTO> getTopAttractionsByCentrality(String cityName);
}