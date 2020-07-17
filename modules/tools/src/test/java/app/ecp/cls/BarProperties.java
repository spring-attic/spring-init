package app.ecp.cls;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("person.cls")
public class BarProperties {

    private String name = "default";

    public BarProperties() {
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
