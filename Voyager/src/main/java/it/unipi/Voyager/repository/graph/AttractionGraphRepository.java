// AttractionGraphRepository.java
package it.unipi.Voyager.repository.graph;
import it.unipi.Voyager.model.graph.AttractionNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface AttractionGraphRepository extends Neo4jRepository<AttractionNode, String> {}