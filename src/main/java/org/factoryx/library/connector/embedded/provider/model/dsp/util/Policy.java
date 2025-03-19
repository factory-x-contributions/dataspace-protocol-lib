package org.factoryx.library.connector.embedded.provider.model.dsp.util;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Policy {
    private String id;
    private String type;
    private String assigner;
    private List<Permission> permission;
}