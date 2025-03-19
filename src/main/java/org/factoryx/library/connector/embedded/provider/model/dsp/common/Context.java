package org.factoryx.library.connector.embedded.provider.model.dsp.common;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class Context {
    private String title;
    private String type;
    private List<String> items;
    private List<ContextDefinition> allOf;
    private String id;
    private Definitions definitions;

    @Getter
    @Setter
    public static class Definitions {
        private ContextDefinition Context;
    }

    @Getter
    @Setter
    public static class ContextDefinition {
        private String type;
        private List<String> items;
        private String contains;
    }
}