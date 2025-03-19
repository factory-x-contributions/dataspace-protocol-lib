package org.factoryx.library.connector.embedded.provider.model.dsp.catalog;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class Dataset {
    private List<String> context;
    private String id;
    private String type;
    private List<Policy> hasPolicy;
    private List<Distribution> distribution;

    @Getter
    @Setter
    public static class Policy {
        private String type;
        private String id;
        private String assigner;
        private List<Permission> permission;

        @Getter
        @Setter
        public static class Permission {
            private String action;
            private List<Constraint> constraint;

            @Getter
            @Setter
            public static class Constraint {
                private String leftOperand;
                private String rightOperand;
                private String operator;
            }
        }
    }

    @Getter
    @Setter
    public static class Distribution {
        private String type;
        private String format;
        private AccessService accessService;

        @Getter
        @Setter
        public static class AccessService {
            private String id;
            private String type;
            private String endpointURL;
        }
    }
}