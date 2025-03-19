package org.factoryx.library.connector.embedded.provider.model.dsp.catalog;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class CatalogError {
    private List<String> context;
    private String type;
    private String code;
    private List<String> reason;
}