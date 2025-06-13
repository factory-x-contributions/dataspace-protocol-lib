package org.factoryx.library.connector.embedded.repository;

import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;
import org.factoryx.library.connector.embedded.provider.repository.TransferRecordRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MongoTransferRecordRepository extends TransferRecordRepository, MongoRepository<TransferRecord, UUID> {
}
