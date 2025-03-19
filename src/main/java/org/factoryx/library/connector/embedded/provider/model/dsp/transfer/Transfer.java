package org.factoryx.library.connector.embedded.provider.model.dsp.transfer;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class Transfer {
    private List<String> context;
    private String providerPid;
    private String consumerPid;
    private String code;
    private List<String> reason;

    private String type;
    private String endpointType;
    private String endpoint;
    private List<EndpointProperty> endpointProperties;

    @Getter
    @Setter
    public static class EndpointProperty {
        private String type;
        private String name;
        private String value;
    }
}