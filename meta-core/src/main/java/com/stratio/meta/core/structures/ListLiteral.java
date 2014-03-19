package com.stratio.meta.core.structures;

import com.stratio.meta.sh.utils.ShUtils;

import java.util.ArrayList;
import java.util.List;

public class ListLiteral extends IdentIntOrLiteral {
    
    public List<String> literals;

    public ListLiteral() {
        literals = new ArrayList<>();
    }
    
    public ListLiteral(String identifier, char operator, List<String> literals) {
        this();
        this.identifier = identifier;
        this.operator = operator;
        this.literals = literals;
    }
    
    public ListLiteral(List<String> literals) {
        this();
        this.literals = literals;
    }

    public List<String> getLiterals() {
        return literals;
    }

    public void setLiterals(List<String> literals) {
        this.literals = literals;
    }        

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(string());
        sb.append(ShUtils.StringList(literals, ", "));
        return sb.toString();
    }
    
    
    
}
