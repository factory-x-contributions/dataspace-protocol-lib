package org.factoryx.library.connector.embedded.provider.model.dsp.common;
import lombok.Getter;
import lombok.Setter;
import org.factoryx.library.connector.embedded.provider.model.dsp.util.ProtocolVersion;

import java.util.List;

@Getter
@Setter
public class Protocol {
    private List<ProtocolVersion> protocolVersions;
}