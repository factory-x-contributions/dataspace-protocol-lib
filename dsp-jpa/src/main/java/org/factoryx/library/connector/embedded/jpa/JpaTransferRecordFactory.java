package org.factoryx.library.connector.embedded.jpa;

import org.factoryx.library.connector.embedded.model.JpaTransferRecord;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;
import org.factoryx.library.connector.embedded.provider.service.TransferRecordFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("jpa")
public class JpaTransferRecordFactory implements TransferRecordFactory {

    @Override
    public TransferRecord create() {
        return new JpaTransferRecord();
    }
}
