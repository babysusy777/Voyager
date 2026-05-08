package it.unipi.Voyager.config;

import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
public class MongoCollections {

    private final MongoClient mongoClient;

    public MongoCollections(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public MongoDatabase strongDatabase() {
        return mongoClient
                .getDatabase("voyager")
                .withReadPreference(ReadPreference.primary())
                .withReadConcern(ReadConcern.MAJORITY)
                .withWriteConcern(WriteConcern.MAJORITY.withJournal(true));
    }

    public MongoDatabase fastDatabase() {
        return mongoClient
                .getDatabase("voyager")
                .withReadPreference(ReadPreference.secondaryPreferred())
                .withReadConcern(ReadConcern.LOCAL)
                .withWriteConcern(WriteConcern.W1);
    }

    public MongoCollection<Document> strongCollection(String collectionName) {
        return strongDatabase().getCollection(collectionName);
    }

    public MongoCollection<Document> fastCollection(String collectionName) {
        return fastDatabase().getCollection(collectionName);
    }
}
