package org.factoryx.library.connector.embedded.provider.model.dsp.negotiation;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ContractAgreementMessage {
    private List<String> context;
    private String type;
    private String providerPid;
    private String consumerPid;
    private Agreement agreement;
    private String callbackAddress;

    @Getter
    @Setter
    public static class Agreement {
        private String id;
        private String type;
        private String target;
        private String profile;
        private String timestamp;
        private String assigner;
        private String assignee;
        private List<Permission> permission;

        @Getter
        @Setter
        public static class Permission {
            private String action;
            private List<Constraint> constraint;
            private Duty duty;

            @Getter
            @Setter
            public static class Duty {
                private String action;
                private List<Constraint> constraint;
            }

            @Getter
            @Setter
            public static class Constraint {
                private String leftOperand;
                private String operator;
                private String rightOperand;
                private List<Constraint> and;
                private List<Constraint> or;
                private List<Constraint> andSequence;
                private List<Constraint> xone;
            }
        }
    }
}