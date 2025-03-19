package org.factoryx.library.connector.embedded.provider.model.dsp.catalog;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class CatalogService {
    private List<String> context;
    private String id;
    private String type;
    private String serviceEndpoint;
}