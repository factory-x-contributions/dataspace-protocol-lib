package org.factoryx.library.connector.embedded.provider.model.dsp.util;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Agreement {
    private String id;
    private String type;
    private String target;
    private String profile;
    private String timestamp;
    private String assigner;
    private String assignee;
    private List<Permission> permission;
}