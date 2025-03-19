package org.factoryx.library.connector.embedded.provider.model.dsp.catalog;

import lombok.Getter;
import lombok.Setter;
import org.factoryx.library.connector.embedded.provider.model.dsp.common.Context;

@Getter
@Setter
public class DatasetRequestMessage {
    private Context context;
    private String type;
    private String dataset;
}