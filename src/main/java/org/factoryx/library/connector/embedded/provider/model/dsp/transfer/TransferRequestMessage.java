package org.factoryx.library.connector.embedded.provider.model.dsp.transfer;
import lombok.Getter;
import lombok.Setter;
import org.factoryx.library.connector.embedded.provider.model.dsp.util.DataAddress;

import java.util.List;

@Getter
@Setter
public class TransferRequestMessage {
    private List<String> context;
    private String type;
    private String consumerPid;
    private String agreementId;
    private String format;
    private DataAddress dataAddress;
    private String callbackAddress;
}