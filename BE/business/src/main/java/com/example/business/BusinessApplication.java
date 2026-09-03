package com.example.business;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// com.logiccheck 하위 도메인(review/ai)을 함께 스캔한다.
// 개발자1이 뼈대를 com.logiccheck 로 옮기면 scanBasePackages 는 필요 없다.
@SpringBootApplication(scanBasePackages = {"com.example.business", "com.logiccheck"})
public class BusinessApplication {

	public static void main(String[] args) {
		SpringApplication.run(BusinessApplication.class, args);
	}

}
