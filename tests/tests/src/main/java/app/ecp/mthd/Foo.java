package app.ecp.mthd;

public class Foo {

    private String value;

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
