package it.unipi.Voyager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
        basePackages = "it.unipi.Voyager.repository.strong",
        mongoTemplateRef = "strongMongoTemplate"
)
public class StrongMongoRepositoryConfig {
}