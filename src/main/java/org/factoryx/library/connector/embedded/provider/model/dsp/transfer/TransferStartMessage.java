package org.factoryx.library.connector.embedded.provider.model.dsp.transfer;

import lombok.Getter;
import lombok.Setter;
import org.factoryx.library.connector.embedded.provider.model.dsp.common.Context;
import org.factoryx.library.connector.embedded.provider.model.dsp.util.DataAddress;

@Getter
@Setter
public class TransferStartMessage {
    private Context context;
    private String type;
    private String providerPid;
    private String consumerPid;
    private DataAddress dataAddress;
}