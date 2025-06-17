package org.factoryx.library.connector.embedded.repository;

import org.factoryx.library.connector.embedded.model.JapTransferRecord;
import org.factoryx.library.connector.embedded.provider.repository.TransferRecordRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Primary
@Repository
public interface JapTransferRecordRepository extends JpaRepository<JapTransferRecord, UUID>, TransferRecordRepository {
    @Override
    Optional<JapTransferRecord> findById(UUID id);

}
