package app.init;

public class Bar {

    private final Foo foo;

    public Bar(Foo foo) {
        this.foo = foo;
    }

    public Foo getFoo() {
        return this.foo;
    }
    
    public void start() {
    	this.foo.setValue("*" + this.foo.getValue() + "*");
    }
}
