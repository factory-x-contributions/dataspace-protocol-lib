package org.factoryx.library.connector.embedded.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferState;

import java.util.UUID;

@Getter
@Setter
@Slf4j
@ToString
@Entity
public class JapTransferRecord extends TransferRecord {
    /**
     * The transfer id on the Provider side (our side)
     * <p>
     * Is always assigned by the service. Never set manually!
     */
    @Id
    @GeneratedValue()
    private UUID ownPid;

    /**
     * The transfer id on the Consumer side
     */
    private String consumerPid;

    /**
     * The id of the consumer partner
     */
    private String partnerId;

    /**
     * The URI indicating where messages to the Consumer should be sent
     */
    private String partnerDspUrl;

    /**
     * The credentials, which our partner has sent us during this transfer
     */
    private String partnerCredentials;

    /**
     * The id of the agreement, which is the basis for this transfer
     */
    private String contractId;

    /**
     * The id of the dataset, which is the subject of this transfer
     */
    private UUID datasetId;

    /**
     * The format of the transfer, usually "HTTP_PUSH" or "HTTP_PULL"
     */
    private String format;

    /**
     * The endpoint to which the data should be transferred
     */
    private String datasetAddressUrl;

    /**
     * The current state of the transfer
     */
    private TransferState state;
}
