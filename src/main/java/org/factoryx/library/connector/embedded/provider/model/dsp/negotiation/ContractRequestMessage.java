package org.factoryx.library.connector.embedded.provider.model.dsp.negotiation;
import lombok.Getter;
import lombok.Setter;
import org.factoryx.library.connector.embedded.provider.model.dsp.util.Offer;

import java.util.List;

@Getter
@Setter
public class ContractRequestMessage {
    private List<String> context;
    private String type;
    private String providerPid;
    private String consumerPid;
    private Offer offer;
    private String callbackAddress;
}