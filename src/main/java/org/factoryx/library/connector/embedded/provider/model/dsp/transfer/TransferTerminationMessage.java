package org.factoryx.library.connector.embedded.provider.model.dsp.transfer;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class TransferTerminationMessage {
    private List<String> context;
    private String type;
    private String providerPid;
    private String consumerPid;
    private String code;
    private List<String> reason;
}