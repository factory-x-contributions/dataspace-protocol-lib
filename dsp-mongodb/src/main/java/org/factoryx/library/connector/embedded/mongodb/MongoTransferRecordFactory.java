package org.factoryx.library.connector.embedded.mongodb;

import org.factoryx.library.connector.embedded.model.MongoTransferRecord;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;
import org.factoryx.library.connector.embedded.provider.service.TransferRecordFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mongodb")
public class MongoTransferRecordFactory implements TransferRecordFactory {
    @Override
    public TransferRecord create() {
        return new MongoTransferRecord();
    }
}
