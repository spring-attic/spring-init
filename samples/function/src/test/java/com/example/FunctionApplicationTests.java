package com.example;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.WebHandler;

@RunWith(SpringRunner.class)
@SpringBootTest("spring.functional.enabled=false")
public class FunctionApplicationTests {

	@Autowired
	private WebHandler webHandler;

	private WebTestClient client;

	@Before
	public void init() {
		client = WebTestClient.bindToWebHandler(webHandler).build();
	}

	@Test
	public void test() {
		client.post().uri("/").syncBody("foo").exchange().expectBody(String.class)
				.isEqualTo("FOO");
	}

}
