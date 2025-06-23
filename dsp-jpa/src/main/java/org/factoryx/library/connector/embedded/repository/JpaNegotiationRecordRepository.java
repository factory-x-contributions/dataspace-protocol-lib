package org.factoryx.library.connector.embedded.repository;

import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.model.JpaNegotiationRecord;
import org.factoryx.library.connector.embedded.provider.repository.NegotiationRecordRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;


@Repository
@Profile("jpa")
public interface JpaNegotiationRecordRepository extends JpaRepository<JpaNegotiationRecord, UUID>, NegotiationRecordRepository {

    @Override
    List<NegotiationRecord> findAllByContractId(UUID contractId);

    @Override
    Optional<JpaNegotiationRecord> findById(UUID id);
}