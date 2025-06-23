package org.factoryx.library.connector.embedded.mongodb;

import org.factoryx.library.connector.embedded.model.MongoNegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.service.NegotiationRecordFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("mongodb")
public class MongoNegotiationRecordFactory implements NegotiationRecordFactory {

    @Override
    public NegotiationRecord create() {
        MongoNegotiationRecord record = new MongoNegotiationRecord();
        record.setOwnPid(UUID.randomUUID());
        return record;
    }
}
