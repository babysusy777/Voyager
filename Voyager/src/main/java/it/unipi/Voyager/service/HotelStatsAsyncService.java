package it.unipi.Voyager.service;

import it.unipi.Voyager.config.DatabaseInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class HotelStatsAsyncService {

    @Autowired
    private DatabaseInitializer databaseInitializer;

    @Async
    public void recomputeAllStatsAsync() {
        System.out.println("[Stats] Soglia raggiunta — ricalcolo statistiche...");
        databaseInitializer.updateCityIndexes();
        System.out.println("[Stats] Ricalcolo completato.");
    }
}
