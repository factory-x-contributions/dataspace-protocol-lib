package org.factoryx.library.connector.embedded.provider.model.dsp.util;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Permission {
    private String action;
    private List<Constraint> constraint;
    private Duty duty;
}