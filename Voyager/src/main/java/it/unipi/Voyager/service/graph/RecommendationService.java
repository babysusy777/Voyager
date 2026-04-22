package it.unipi.Voyager.service.graph;

import it.unipi.Voyager.dto.TravellerSimilarityDTO;
import it.unipi.Voyager.dto.TripSummaryDTO;
import it.unipi.Voyager.model.graph.TravellerNode;
import it.unipi.Voyager.repository.graph.TravellerGraphRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {

    @Autowired
    private TravellerGraphRepository travellerGraphRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    private List<TripSummaryDTO> getTripsByEmail(String email) {
        return neo4jClient.query(
                        "MATCH (t:Traveller {email: $email})-[:MADE_TRIP]->(trip:Trip) " +
                                "OPTIONAL MATCH (trip)-[:STAYED_AT]->(hotel:Hotel)-[:LOCATED_IN]->(city:City) " +
                                "RETURN trip.ratingGiven AS ratingGiven, hotel.hotelName AS hotelName, city.cityName AS cityName"
                )
                .bind(email).to("email")
                .fetchAs(TripSummaryDTO.class)
                .mappedBy((typeSystem, record) -> new TripSummaryDTO(
                        record.get("hotelName").isNull() ? null : record.get("hotelName").asString(),
                        record.get("cityName").isNull() ? null : record.get("cityName").asString(),
                        record.get("ratingGiven").asInt()
                ))
                .all()
                .stream().toList();
    }

    public List<TravellerSimilarityDTO> getSuggestions(String email) {
        List<TravellerNode> similar = travellerGraphRepository.findTopSimilarTravellers(email);
        return similar.stream().map(t -> {
            List<TripSummaryDTO> tripDTOs = getTripsByEmail(t.getEmail());
            return new TravellerSimilarityDTO(
                    t.getEmail(), t.getAge(), t.getGender(),
                    t.getUserSegment(), t.getPreferencesSeason(), tripDTOs
            );
        }).toList();
    }
}
