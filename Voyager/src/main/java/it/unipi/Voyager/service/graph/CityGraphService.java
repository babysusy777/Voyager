package it.unipi.Voyager.service.graph;

import it.unipi.Voyager.repository.graph.CityGraphRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CityGraphService {

    @Autowired
    private CityGraphRepository cityGraphRepository;

    public List<Map<String, Object>> getSimilarCities(String cityName) {
        return cityGraphRepository.findSimilarCities(cityName);
    }

}
