package org.factoryx.library.connector.embedded.repository;

import org.factoryx.library.connector.embedded.model.MongoNegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.repository.NegotiationRecordRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MongoNegotiationRecordRepository extends MongoRepository<MongoNegotiationRecord, UUID>, NegotiationRecordRepository {
    @Override
    List<NegotiationRecord> findAllByContractId(UUID contractId);

    @Override
    Optional<MongoNegotiationRecord> findById(UUID id);
}