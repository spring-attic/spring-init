package app.main.foo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Foo {

	@JsonIgnore
	private Long id;

	private String value;

	public Foo() {
	}

	public Foo(String value) {
		this.value = value;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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
