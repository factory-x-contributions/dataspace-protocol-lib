package org.factoryx.library.connector.embedded.provider.model.dsp.common;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class Protocol {
    private List<ProtocolVersion> protocolVersions;

    @Getter
    @Setter
    public static class ProtocolVersion {
        private String version;
        private String path;
    }
}