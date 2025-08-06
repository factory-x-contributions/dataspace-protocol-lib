package org.factoryx.library.connector.embedded.teststarter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.factoryx.library.connector.embedded.provider.interfaces.DataAsset;

import java.util.Map;
import java.util.UUID;

public class SampleDataAsset implements DataAsset {
    private final static ObjectMapper MAPPER = new ObjectMapper();

    private UUID id;

    private final String fieldA = "fieldA" + UUID.randomUUID();

    private final String fieldB = "fieldB" + UUID.randomUUID();

    public SampleDataAsset(UUID id) {
        this.id = id;
    }

    public SampleDataAsset() {
        this(UUID.randomUUID());
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public Map<String, String> getProperties() {
        return Map.of("type", "SampleDataAsset", "hasFieldA", "true", "hasFieldB", "true");
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public byte[] getDtoRepresentation() {
        try {
            return MAPPER.writeValueAsBytes(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
