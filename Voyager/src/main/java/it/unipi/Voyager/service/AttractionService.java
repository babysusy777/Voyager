package it.unipi.Voyager.service;

import it.unipi.Voyager.dto.AttractionCentralityDTO;
import it.unipi.Voyager.repository.AttractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttractionService {

    @Autowired
    private AttractionRepository attractionRepository;

    public List<AttractionCentralityDTO> getTopAttractions(String cityName) {
        return attractionRepository.getTopAttractionsByCentrality(cityName);
    }
}