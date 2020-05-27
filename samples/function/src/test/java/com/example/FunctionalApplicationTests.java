package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.WebHandler;

@SpringBootTest("spring.functional.enabled=true")
public class FunctionalApplicationTests {

	@Autowired
	private WebHandler webHandler;

	private WebTestClient client;

	@BeforeEach
	public void init() {
		client = WebTestClient.bindToWebHandler(webHandler).build();
	}

	@Test
	public void test() {
		client.post().uri("/").bodyValue("foo").exchange().expectBody(String.class).isEqualTo("FOO");
	}

}
