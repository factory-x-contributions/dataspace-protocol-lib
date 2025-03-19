package org.factoryx.library.connector.embedded.provider.model.dsp.util;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Constraint {
    private String leftOperand;
    private String operator;
    private String rightOperand;
}