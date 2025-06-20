package org.factoryx.library.connector.embedded.repository;

import org.factoryx.library.connector.embedded.model.JpaTransferRecord;
import org.factoryx.library.connector.embedded.provider.repository.TransferRecordRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@Profile("jpa")
public interface JpaTransferRecordRepository extends JpaRepository<JpaTransferRecord, UUID>, TransferRecordRepository {
    @Override
    Optional<JpaTransferRecord> findById(UUID id);

}
