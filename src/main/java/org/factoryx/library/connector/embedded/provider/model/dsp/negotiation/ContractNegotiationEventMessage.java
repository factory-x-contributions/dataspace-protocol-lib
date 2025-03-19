package org.factoryx.library.connector.embedded.provider.model.dsp.negotiation;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ContractNegotiationEventMessage {
    private List<String> context;
    private String type;
    private String providerPid;
    private String consumerPid;
    private String eventType;
}