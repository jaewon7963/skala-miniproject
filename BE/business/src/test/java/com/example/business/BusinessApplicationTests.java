package com.example.business;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Postgres + stub 프로파일이 필요하다. 컨텍스트 로딩은 `./gradlew bootRun --args='--spring.profiles.active=local,stub'` 으로 검증한다.")
@SpringBootTest
class BusinessApplicationTests {

	@Test
	void contextLoads() {
	}

}
