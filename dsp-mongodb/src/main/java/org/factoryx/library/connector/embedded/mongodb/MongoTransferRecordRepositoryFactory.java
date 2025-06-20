package org.factoryx.library.connector.embedded.mongodb;

import org.factoryx.library.connector.embedded.provider.repository.TransferRecordRepository;
import org.factoryx.library.connector.embedded.provider.repository.TransferRecordRepositoryFactory;
import org.factoryx.library.connector.embedded.repository.MongoTransferRecordRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mongodb")
public class MongoTransferRecordRepositoryFactory implements TransferRecordRepositoryFactory {

    private final MongoTransferRecordRepository repository;

    public MongoTransferRecordRepositoryFactory(MongoTransferRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public TransferRecordRepository getRepository() {
        return repository;
    }
}
