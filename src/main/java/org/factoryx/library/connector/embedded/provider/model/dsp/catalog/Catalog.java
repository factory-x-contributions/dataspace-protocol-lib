package org.factoryx.library.connector.embedded.provider.model.dsp.catalog;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class Catalog {
    private List<String> context;
    private String id;
    private String type;
    private String participantId;
    private List<Service> service;
    private List<Dataset> dataset;

    @Getter
    @Setter
    public static class Service {
        private String id;
        private String type;
        private String endpointURL;
    }

    @Getter
    @Setter
    public static class Dataset {
        private String id;
        private String type;
        private List<Policy> hasPolicy;
        private List<Distribution> distribution;

        @Getter
        @Setter
        public static class Policy {
            private String id;
            private String type;
            private List<Permission> permission;
        }

        @Getter
        @Setter
        public static class Permission {
            private String action;
            private List<Constraint> constraint;
            private Duty duty;

            @Getter
            @Setter
            public static class Constraint {
                private String leftOperand;
                private String operator;
                private String rightOperand;
            }
        }

        @Getter
        @Setter
        public static class Duty {
            private String action;
        }

        @Getter
        @Setter
        public static class Distribution {
            private String type;
            private String format;
            private String accessService;
        }
    }
}