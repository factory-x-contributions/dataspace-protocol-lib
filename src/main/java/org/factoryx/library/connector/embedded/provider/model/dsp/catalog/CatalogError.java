package org.factoryx.library.connector.embedded.provider.model.dsp.catalog;

import lombok.Getter;
import lombok.Setter;
import org.factoryx.library.connector.embedded.provider.model.dsp.common.Context;

import java.util.List;

@Getter
@Setter
public class CatalogError {
    private Context context;
    private String type;
    private String code;
    private List<String> reason;
}