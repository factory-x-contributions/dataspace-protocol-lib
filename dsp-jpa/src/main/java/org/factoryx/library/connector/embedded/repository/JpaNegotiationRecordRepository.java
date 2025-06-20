package org.factoryx.library.connector.embedded.repository;

import org.factoryx.library.connector.embedded.model.JpaNegotiationRecord;
import org.factoryx.library.connector.embedded.provider.repository.NegotiationRecordRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;


@Primary
@Repository
@Profile("jpa")
public interface JpaNegotiationRecordRepository extends JpaRepository<JpaNegotiationRecord, UUID>, NegotiationRecordRepository {
    @Override
    List<JpaNegotiationRecord> findAllByContractId(UUID contractId);

    @Override
    Optional<JpaNegotiationRecord> findById(UUID id);
}