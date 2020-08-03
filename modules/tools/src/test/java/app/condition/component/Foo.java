package app.condition.component;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConditionalOnClass(String.class)
@ConfigurationProperties("app")
public class Foo {

    private String value;

    public Foo() {
    }

    public Foo(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Foo [value=" + this.value + "]";
    }

}
