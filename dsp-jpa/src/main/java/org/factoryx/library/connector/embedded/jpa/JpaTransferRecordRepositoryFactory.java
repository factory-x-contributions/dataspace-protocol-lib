package org.factoryx.library.connector.embedded.jpa;

import org.factoryx.library.connector.embedded.provider.repository.TransferRecordRepository;
import org.factoryx.library.connector.embedded.provider.repository.TransferRecordRepositoryFactory;
import org.factoryx.library.connector.embedded.repository.JpaTransferRecordRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("jpa")
public class JpaTransferRecordRepositoryFactory implements TransferRecordRepositoryFactory {

    private final JpaTransferRecordRepository repository;

    public JpaTransferRecordRepositoryFactory(JpaTransferRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public TransferRecordRepository getRepository() {
        return repository;
    }
}
