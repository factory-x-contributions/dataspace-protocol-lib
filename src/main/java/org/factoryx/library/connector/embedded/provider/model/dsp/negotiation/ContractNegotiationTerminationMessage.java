package org.factoryx.library.connector.embedded.provider.model.dsp.negotiation;

import lombok.Getter;
import lombok.Setter;
import org.factoryx.library.connector.embedded.provider.model.dsp.common.Context;

import java.util.List;

@Getter
@Setter
public class ContractNegotiationTerminationMessage {
    private Context context;
    private String type;
    private String providerPid;
    private String consumerPid;
    private String code;
    private List<String> reason;
}