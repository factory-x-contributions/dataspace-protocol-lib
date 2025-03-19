package org.factoryx.library.connector.embedded.provider.model.dsp.catalog;

import lombok.Getter;
import lombok.Setter;
import org.factoryx.library.connector.embedded.provider.model.dsp.util.Service;

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
}