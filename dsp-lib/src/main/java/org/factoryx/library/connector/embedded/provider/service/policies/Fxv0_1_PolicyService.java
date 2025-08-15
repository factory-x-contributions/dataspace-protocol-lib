package org.factoryx.library.connector.embedded.provider.service.policies;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonValue;
import org.factoryx.library.connector.embedded.provider.interfaces.DspPolicyService;
import org.factoryx.library.connector.embedded.provider.model.DspVersion;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "org.factoryx.library.policyservice", havingValue = "fxv0_1")
public class Fxv0_1_PolicyService extends DspPolicyService {

    protected Fxv0_1_PolicyService(EnvService envService) {
        super(envService);
    }


    /**
     * Returns a permission JSON structure based on the hash value of assetId.
     * The output varies but is not random; it depends on whether the hash value is even or odd.
     *
     * @param assetId the id of the asset
     * @param partnerId the id of the negotiation partner
     * @return A JSON representation of the permission policy.
     */
    @Override
    public JsonArray getPermission(String assetId, String partnerId, DspVersion version) {
        int hash = assetId.hashCode();
        String prefix = DspVersion.V_08.equals(version) ? "odrl:" : "";

        if (hash % 2 == 0) {
            return Json.createArrayBuilder().add(Json.createObjectBuilder()
                    .add(prefix  +"action", Json.createObjectBuilder()
                            .add("@id", prefix + "use"))
                    .add(prefix + "constraint", Json.createObjectBuilder()
                            .add(prefix + "leftOperand", Json.createObjectBuilder()
                                    .add("@id", "https://w3id.org/factory-x/policy/v1.0/MembershipConstraint"))
                            .add(prefix + "operator", Json.createObjectBuilder()
                                    .add("@id", prefix + "eq"))
                            .add(prefix + "rightOperand", "active"))
                    .build()).build();
        } else {
            return Json.createArrayBuilder().add(Json.createObjectBuilder()
                    .add(prefix + "permission", Json.createObjectBuilder()
                            .add(prefix + "action", Json.createObjectBuilder()
                                    .add("@id",prefix +  "use"))
                            .add(prefix + "constraint", Json.createObjectBuilder()
                                    .add(prefix + "leftOperand", Json.createObjectBuilder()
                                            .add("@id", "https://w3id.org/factoryx/policy/certification"))
                                    .add(prefix + "operator", Json.createObjectBuilder()
                                            .add("@id",prefix +  "eq"))
                                    .add(prefix + "rightOperand", "MyCertification")))
                    .build()).build();
        }
    }
}
