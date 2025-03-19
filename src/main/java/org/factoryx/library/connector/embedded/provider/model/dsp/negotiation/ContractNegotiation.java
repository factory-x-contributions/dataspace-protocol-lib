package org.factoryx.library.connector.embedded.provider.model.dsp.negotiation;

import lombok.Getter;
import lombok.Setter;
import org.factoryx.library.connector.embedded.provider.model.dsp.common.Context;

@Getter
@Setter
public class ContractNegotiation {
    private Context context;
    private String type;
    private String providerPid;
    private String consumerPid;
    private String state;
}