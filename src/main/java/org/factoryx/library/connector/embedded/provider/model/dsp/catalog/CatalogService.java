package org.factoryx.library.connector.embedded.provider.model.dsp.catalog;

import lombok.Getter;
import lombok.Setter;
import org.factoryx.library.connector.embedded.provider.model.dsp.common.Context;

@Getter
@Setter
public class CatalogService {
    private Context context;
    private String id;
    private String type;
    private String serviceEndpoint;
}