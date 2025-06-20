package org.factoryx.library.connector.embedded.repository;

import org.factoryx.library.connector.embedded.model.MongoTransferRecord;
import org.factoryx.library.connector.embedded.provider.repository.TransferRecordRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@Profile("mongodb")
public interface MongoTransferRecordRepository extends MongoRepository<MongoTransferRecord, UUID>, TransferRecordRepository {
    @Override
    Optional<MongoTransferRecord> findById(UUID id);
}
