package it.unipi.Voyager.service.graph;

import it.unipi.Voyager.model.graph.TravellerNode;
import it.unipi.Voyager.repository.graph.TravellerGraphRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendationService {

    @Autowired
    private TravellerGraphRepository travellerGraphRepository;

    public void updateSimilarityNetwork(String travellerId) {
        travellerGraphRepository.computeAndSaveSimilarity(travellerId);
    }

    public List<TravellerNode> getSuggestions(String email) {
        travellerGraphRepository.computeAndSaveSimilarity(email);
        return travellerGraphRepository.findTopSimilarTravellers(email);
    }
}
