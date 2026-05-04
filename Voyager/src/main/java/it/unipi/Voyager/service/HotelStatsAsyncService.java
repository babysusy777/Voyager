package it.unipi.Voyager.service;

import it.unipi.Voyager.config.DatabaseInitializer;
import it.unipi.Voyager.config.Neo4jSyncService;
import it.unipi.Voyager.repository.fast.HotelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class HotelStatsAsyncService {

    @Autowired
    private DatabaseInitializer databaseInitializer;

    @Autowired
    private Neo4jSyncService neo4jSyncService;

    @Autowired
    private CityService cityService;

    @Autowired
    private HotelRepository hotelRepository;

    @Async
    public void recomputeAllStatsAsync(
            Optional<String> email,
            Optional<String> hotelId,
            Optional<String> hotelName,
            Optional<String> cityName
    ) {
        System.out.println("[Stats] Soglia raggiunta — ricalcolo statistiche...");

        if (email.isPresent()) {
            cityService.removeHotelFromCityMetrics(
                    cityName.get(),
                    hotelId.get(),
                    hotelName.get()
            );

            hotelRepository.deleteById(hotelId.get());

        } else {
            databaseInitializer.updateCityIndexes();
        }

        neo4jSyncService.syncAll();

        System.out.println("[Stats] Ricalcolo completato.");
    }
}
