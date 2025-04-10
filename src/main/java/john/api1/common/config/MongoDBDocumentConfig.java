package john.api1.common.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Getter
@Configuration
@EnableMongoAuditing
@PropertySource("classpath:application.properties")
public class MongoDBDocumentConfig {

    @Value("${mongodb.collection.member-accounts}")
    private String memberAccounts;

    @Value("${mongodb.collection.member-photos}")
    private String memberPhotos;

    @Value("${mongodb.collection.employee-accounts}")
    private String employeeAccounts;

    @Value("${mongodb.collection.employee-photos}")
    private String employeePhotos;
}
