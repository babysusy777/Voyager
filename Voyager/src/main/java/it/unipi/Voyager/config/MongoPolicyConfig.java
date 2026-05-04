package it.unipi.Voyager.config;

import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
public class MongoPolicyConfig {

    @Bean
    public MongoClient mongoClient(
            @Value("${spring.data.mongodb.uri}") String mongoUri
    ) {
        return MongoClients.create(mongoUri);
    }

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        return new SimpleMongoClientDatabaseFactory(mongoClient, "travel_db");
    }

    @Primary
    @Bean(name = "strongMongoTemplate")
    public MongoTemplate strongMongoTemplate(MongoDatabaseFactory factory) {
        MongoTemplate template = new MongoTemplate(factory);

        template.setReadPreference(ReadPreference.primary());
        template.setWriteConcern(WriteConcern.MAJORITY.withJournal(true));

        return template;
    }

    @Bean(name = "fastMongoTemplate")
    public MongoTemplate fastMongoTemplate(MongoDatabaseFactory factory) {
        MongoTemplate template = new MongoTemplate(factory);

        template.setReadPreference(ReadPreference.secondaryPreferred());
        template.setWriteConcern(WriteConcern.W1);

        return template;
    }
}
