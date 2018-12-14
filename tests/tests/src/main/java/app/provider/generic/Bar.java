package app.provider.generic;

public class Bar<T> {

    private final T foo;

    public Bar(T foo) {
        this.foo = foo;
    }

    public T getFoo() {
        return this.foo;
    }
}
