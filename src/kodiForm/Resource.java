package kodiForm;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Created by mike on 28.10.15.
 */
class Resource {
    private final StringProperty resource;

    public Resource(String resource) {
        this.resource = new SimpleStringProperty(resource);
    }

    public String getResource() {
        return resource.get();
    }

    public StringProperty resourceProperty() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource.set(resource);
    }

    @Override
    public String toString() {
        return getResource();
    }
}

