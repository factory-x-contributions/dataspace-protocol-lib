package org.factoryx.library.connector.embedded.provider.model.dsp.util;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Distribution {
    private String type;
    private String format;
    private AccessService accessService;
}