package org.factoryx.library.connector.embedded.provider.service.dsp_validation.fxvalidation_v0_1;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.factoryx.library.connector.embedded.provider.interfaces.DspTokenProviderService;
import org.factoryx.library.connector.embedded.provider.model.negotiation.NegotiationRecord;
import org.factoryx.library.connector.embedded.provider.model.transfer.TransferRecord;

public abstract class FXv0_1_AbstractTokenProviderService implements DspTokenProviderService {

    @Override
    abstract public String provideTokenForPartner(NegotiationRecord record);

    @Override
    abstract public String provideTokenForPartner(TransferRecord record);

    abstract String getWrappedToken(String partnerDid, String tokenFromPartner);
}
