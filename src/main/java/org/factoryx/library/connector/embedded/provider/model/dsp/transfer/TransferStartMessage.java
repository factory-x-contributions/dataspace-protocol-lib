package org.factoryx.library.connector.embedded.provider.model.dsp.transfer;
import lombok.Getter;
import lombok.Setter;
import org.factoryx.library.connector.embedded.provider.model.dsp.util.DataAddress;

import java.util.List;

@Getter
@Setter
public class TransferStartMessage {
    private List<String> context;
    private String type;
    private String providerPid;
    private String consumerPid;
    private DataAddress dataAddress;
}