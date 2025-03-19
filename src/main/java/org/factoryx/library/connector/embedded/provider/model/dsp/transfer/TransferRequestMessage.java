package org.factoryx.library.connector.embedded.provider.model.dsp.transfer;

import lombok.Getter;
import lombok.Setter;
import org.factoryx.library.connector.embedded.provider.model.dsp.common.Context;
import org.factoryx.library.connector.embedded.provider.model.dsp.util.DataAddress;

@Getter
@Setter
public class TransferRequestMessage {
    private Context context;
    private String type;
    private String consumerPid;
    private String agreementId;
    private String format;
    private DataAddress dataAddress;
    private String callbackAddress;
}