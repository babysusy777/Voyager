package it.unipi.Voyager.service.graph;


import it.unipi.Voyager.repository.graph.TravellerGraphRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class TravellerGraphService {
    @Autowired
    private TravellerGraphRepository travellerNodeRepository; // Bulk — chiamato all'inizializzazione

    public void initializeTravelTypes() {
        travellerNodeRepository.computeAndStoreTravelTypeAll();
    }

    // Singolo — chiamato dopo upsertTrip
    public String updateTravelType(String email) {
        return travellerNodeRepository.computeAndStoreTravelType(email);
    }
}
