package org.factoryx.library.connector.embedded.jpa;

import org.factoryx.library.connector.embedded.provider.repository.NegotiationRecordRepository;
import org.factoryx.library.connector.embedded.provider.repository.NegotiationRecordRepositoryFactory;
import org.factoryx.library.connector.embedded.repository.JpaNegotiationRecordRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("jpa")
public class JpaNegotiationRecordRepositoryFactory implements NegotiationRecordRepositoryFactory {

    private final JpaNegotiationRecordRepository repository;

    public JpaNegotiationRecordRepositoryFactory(JpaNegotiationRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public NegotiationRecordRepository getRepository() {
        return repository;
    }
}
