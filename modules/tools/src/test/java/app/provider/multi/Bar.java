package app.provider.multi;

public class Bar<S, T> {

	private final S pre;

	private final T foo;

	public Bar(S pre, T foo) {
		this.pre = pre;
		this.foo = foo;
	}

	public T getFoo() {
		return this.foo;
	}

	public S getPre() {
		return pre;
	}
}
