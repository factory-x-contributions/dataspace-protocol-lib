package org.factoryx.library.connector.embedded.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Getter
@Setter
@Slf4j
@ToString
@Document("negotiation_records")
@Profile("mongodb")
public class MongoNegotiationRecord extends NegotiationRecord {

    @Override
    @Id
    public UUID getOwnPid() {
        return super.getOwnPid();
    }

    @Override
    public void setOwnPid(UUID ownPid) {
        super.setOwnPid(ownPid);
    }
}
