package org.factoryx.library.connector.embedded.provider.model.dsp.util;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Offer {
    private String type;
    private String id;
    private String target;
    private List<Permission> permission;
}