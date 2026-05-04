package it.unipi.Voyager.service.graph;

import it.unipi.Voyager.dto.RecommendationDTO;
import it.unipi.Voyager.repository.graph.TravellerGraphRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendationService {

    @Autowired
    private TravellerGraphRepository travellerGraphRepository;

    public List<RecommendationDTO> getSuggestions(String email) {
        return travellerGraphRepository.getPersonalizedRecommendations(email);
    }
}