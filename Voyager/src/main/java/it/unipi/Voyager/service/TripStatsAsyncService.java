package it.unipi.Voyager.service;

import it.unipi.Voyager.config.DatabaseInitializer;
import it.unipi.Voyager.config.Neo4jSyncService;
import it.unipi.Voyager.repository.fast.HotelRepository;
import it.unipi.Voyager.repository.graph.TravellerGraphRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
public class TripStatsAsyncService {

    @Autowired
    private DatabaseInitializer databaseInitializer;

    @Autowired
    private Neo4jSyncService neo4jSyncService;

    @Autowired
    private CityService cityService;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private TravellerGraphRepository travellerNodeRepository;

    @Async
    public void recomputeAllStats() {
        System.out.println("[Stats] Soglia raggiunta — ricalcolo statistiche...");

        // Pipeline 1: trip → hotel
        databaseInitializer.populateGuestStats();
        databaseInitializer.populateCityCategoryAvgVisits();
        databaseInitializer.populateSegmentAndPreferenceDistribution();

        // Pipeline 2: trip → city (separata e indipendente)
        databaseInitializer.updateCityIndexes();

        // Neo4j sync
        neo4jSyncService.syncAll();

        travellerNodeRepository.computeAndStoreTravelTypeAll();

        System.out.println("[Stats] Ricalcolo completato.");
    }
}