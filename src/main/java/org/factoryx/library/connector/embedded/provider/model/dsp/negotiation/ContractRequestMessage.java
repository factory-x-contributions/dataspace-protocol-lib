package org.factoryx.library.connector.embedded.provider.model.dsp.negotiation;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ContractRequestMessage {
    private List<String> context;
    private String type;
    private String providerPid;
    private String consumerPid;
    private Offer offer;
    private String callbackAddress;

    @Getter
    @Setter
    public static class Offer {
        private String type;
        private String id;
        private String target;
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
                private String operator;
                private String rightOperand;
            }
        }
    }
}