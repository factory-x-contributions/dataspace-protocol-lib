package org.factoryx.library.connector.embedded.mongodb;

import org.factoryx.library.connector.embedded.provider.repository.NegotiationRecordRepository;
import org.factoryx.library.connector.embedded.provider.repository.NegotiationRecordRepositoryFactory;
import org.factoryx.library.connector.embedded.repository.MongoNegotiationRecordRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mongodb")
public class MongoNegotiationRecordRepositoryFactory implements NegotiationRecordRepositoryFactory {

    private final MongoNegotiationRecordRepository repository;

    public MongoNegotiationRecordRepositoryFactory(MongoNegotiationRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public NegotiationRecordRepository getRepository() {
        return repository;
    }
}
