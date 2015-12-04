package kodiForm;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Created by mike on 28.10.15.
 */
class Device {
    private final StringProperty devName;
    private final StringProperty devURI;
    private final StringProperty devResource;
    private final static String SEP = "|";

    public Device() {
        this(null, null, null);
    }

    public Device(String devName, String devURI, String devResource) {
        this.devName = new SimpleStringProperty(devName == null ? "" : devName);
        this.devURI = new SimpleStringProperty(devURI == null ? "" : devURI);
        this.devResource = new SimpleStringProperty(devResource == null ? "" : devResource);
    }

    public void setDevName(String devName) {
        this.devName.set(devName);
    }

    public String getDevName() {
        return devName.get();
    }

    public StringProperty devNameProperty() {
        return devName;
    }

    public void setDevURI(String devURI) {
        this.devURI.set(devURI);
    }

    public String getDevURI() {
        return devURI.get();
    }

    public StringProperty devURIProperty() {
        return devURI;
    }

    public void setDevResource(String devResource) {
        this.devResource.set(devResource);
    }

    public String getDevResource() {
        return devResource.get();
    }

    public StringProperty devResourceProperty() {
        return devResource;
    }

    @Override
    public String toString() {
        return String.join(SEP, devName.get(), devURI.get(), devResource.get());
    }
}
