package org.factoryx.library.connector.embedded.provider.model.dsp.transfer;

import lombok.Getter;
import lombok.Setter;
import org.factoryx.library.connector.embedded.provider.model.dsp.common.Context;

@Getter
@Setter
public class TransferCompletionMessage {
    private Context context;
    private String type;
    private String providerPid;
    private String consumerPid;
}