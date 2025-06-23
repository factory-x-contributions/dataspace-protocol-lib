package org.factoryx.library.connector.embedded.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Setter
@Getter
@Slf4j
@ToString
@Document("transfer_records")
@Profile("mongodb")
public class MongoTransferRecord extends TransferRecord {

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
