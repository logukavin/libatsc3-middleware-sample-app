package org.ngbp.jsonrpc4jtestharness.models.sample_models.request;

import java.util.List;

public class Properties {
    private List<String> properties;

    public List<String> getProperties() {
        return properties;
    }

    public void setProperties(List<String> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "\n{" +
                "\nproperties: " + properties +
                '}';
    }
}
