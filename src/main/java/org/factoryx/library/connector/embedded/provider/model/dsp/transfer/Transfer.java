package org.factoryx.library.connector.embedded.provider.model.dsp.transfer;

import lombok.Getter;
import lombok.Setter;
import org.factoryx.library.connector.embedded.provider.model.dsp.common.Context;
import org.factoryx.library.connector.embedded.provider.model.dsp.util.EndpointProperty;

import java.util.List;

@Getter
@Setter
public class Transfer {
    private Context context;
    private String providerPid;
    private String consumerPid;
    private String code;
    private List<String> reason;

    private String type;
    private String endpointType;
    private String endpoint;
    private List<EndpointProperty> endpointProperties;
}