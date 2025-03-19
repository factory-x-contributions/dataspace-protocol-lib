package org.factoryx.library.connector.embedded.provider.model.dsp.negotiation;

import lombok.Getter;
import lombok.Setter;
import org.factoryx.library.connector.embedded.provider.model.dsp.common.Context;
import org.factoryx.library.connector.embedded.provider.model.dsp.util.Agreement;

@Getter
@Setter
public class ContractAgreementMessage {
    private Context context;
    private String type;
    private String providerPid;
    private String consumerPid;
    private Agreement agreement;
    private String callbackAddress;
}