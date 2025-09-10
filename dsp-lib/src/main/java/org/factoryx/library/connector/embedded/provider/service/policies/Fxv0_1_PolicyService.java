package org.factoryx.library.connector.embedded.provider.service.policies;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.factoryx.library.connector.embedded.provider.interfaces.DspPolicyService;
import org.factoryx.library.connector.embedded.provider.model.DspVersion;
import org.factoryx.library.connector.embedded.provider.service.helpers.EnvService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "org.factoryx.library.policyservice", havingValue = "fxv0_1")
public class Fxv0_1_PolicyService extends DspPolicyService {

    public Fxv0_1_PolicyService(EnvService envService) {
        super(envService);
    }

    /**
     * Returns a permission JSON structure based according to the latest specification.
     *
     * @param assetId   the id of the asset
     * @param partnerId the id of the negotiation partner
     * @return A JSON representation of the permission policy.
     */
    @Override
    public JsonObject getPermission(String assetId, String partnerId, DspVersion version) {
        String prefix = DspVersion.V_08.equals(version) ? "odrl:" : "";
        var permission = Json.createObjectBuilder();
        permission.add(prefix + "action",
                Json.createObjectBuilder()
                        .add("@id", prefix + "use"));

        permission.add(prefix + "constraint",
                Json.createObjectBuilder()
                        .add(prefix + "leftOperand", Json.createObjectBuilder().add("@id", "https://w3id.org/factoryx/policy/v1.0/Membership"))
                        .add(prefix + "operator", Json.createObjectBuilder().add("@id", prefix + "eq"))
                        .add(prefix + "rightOperand", "active")
        );
        return permission.build();
    }
}
