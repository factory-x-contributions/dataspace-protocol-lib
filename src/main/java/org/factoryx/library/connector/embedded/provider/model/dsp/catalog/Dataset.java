package org.factoryx.library.connector.embedded.provider.model.dsp.catalog;

import lombok.Getter;
import lombok.Setter;
import org.factoryx.library.connector.embedded.provider.model.dsp.common.Context;
import org.factoryx.library.connector.embedded.provider.model.dsp.util.Distribution;
import org.factoryx.library.connector.embedded.provider.model.dsp.util.Policy;

import java.util.List;

@Getter
@Setter
public class Dataset {
    private Context context;
    private String id;
    private String type;
    private List<Policy> hasPolicy;
    private List<Distribution> distribution;
}