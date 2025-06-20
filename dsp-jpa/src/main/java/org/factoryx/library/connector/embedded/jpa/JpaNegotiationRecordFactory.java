package org.factoryx.library.connector.embedded.jpa;

import org.factoryx.library.connector.embedded.model.JpaNegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.service.NegotiationRecordFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("jpa")
public class JpaNegotiationRecordFactory implements NegotiationRecordFactory {
    @Override
    public NegotiationRecord create() {
        return new JpaNegotiationRecord();
    }
}
