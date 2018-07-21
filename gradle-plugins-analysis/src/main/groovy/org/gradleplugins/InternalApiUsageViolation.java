package org.gradleplugins;

import java.util.Objects;

public class InternalApiUsageViolation implements AnalyzeViolation {
    private final String name;

    public InternalApiUsageViolation(String name) {
        this.name = name;
    }

    public String getType() {
        return "internal-api-usage";
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
