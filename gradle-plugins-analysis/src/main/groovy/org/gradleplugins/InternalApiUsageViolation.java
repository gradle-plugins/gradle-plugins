package org.gradleplugins;

import java.util.Objects;

public class InternalApiUsageViolation implements AnalyzeViolation {
    private final String name;
    private final String type = "internal-api-usage";

    public InternalApiUsageViolation(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InternalApiUsageViolation that = (InternalApiUsageViolation) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
