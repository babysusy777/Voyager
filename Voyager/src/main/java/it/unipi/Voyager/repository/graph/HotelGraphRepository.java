// HotelGraphRepository.java
package it.unipi.Voyager.repository.graph;
import it.unipi.Voyager.model.graph.HotelNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import java.util.Optional;

public interface HotelGraphRepository extends Neo4jRepository<HotelNode, Long> {
    Optional<HotelNode> findByHotelName(String hotelName);
}