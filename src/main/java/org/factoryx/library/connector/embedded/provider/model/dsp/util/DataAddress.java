package org.factoryx.library.connector.embedded.provider.model.dsp.util;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DataAddress {
    private String type;
    private String endpointType;
    private String endpoint;
    private List<EndpointProperty> endpointProperties;
}