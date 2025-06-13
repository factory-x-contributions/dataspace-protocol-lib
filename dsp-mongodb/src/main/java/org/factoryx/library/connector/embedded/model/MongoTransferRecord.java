package org.factoryx.library.connector.embedded.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationState;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Setter
@Getter
@Slf4j
@ToString
@Document("transfer_records")
public class MongoTransferRecord extends TransferRecord {
    // Initial attributes (these are not expected to change after the negotiation has started):

    /**
     * The process id, under which we ourselves identify an ongoing negotiation
     * <p>
     * Is always assigned by the service. Never set manually!
     */
    @Id
    private UUID ownPid;
    /**
     * The process id, under which the other partner refers to
     * this negotiation
     */
    private String consumerPid;
    /**
     * The id, under which your partner refers to itself
     */
    private String partnerId;
    /**
     * The protocol URL of your partner
     */
    private String partnerDspUrl;
    /**
     * The id of the asset which is targeted by this negotiation
     */
    private String targetAssetId;
    /**
     * The credentials, which our partner has sent us during this negotiation
     */
    private String partnerCredentials;

    // Attributes that may be set later:
    /**
     * The current state of the negotiation
     */
    private NegotiationState state;
    /**
     * The id of the contract, if the negotiation reaches the AGREED status
     * <p>
     * Is always assigned by the service. Never set manually!
     */
    private UUID contractId;

}
