package org.factoryx.library.connector.embedded.provider.model.dsp.transfer;
import lombok.Getter;
import lombok.Setter;
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

    @Getter
    @Setter
    public static class DataAddress {
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
}