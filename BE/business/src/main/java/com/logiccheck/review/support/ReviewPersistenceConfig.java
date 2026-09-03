// TEMP: 개발자1(global/) 산출물 머지 시 이 파일을 삭제하고 import 를 교체한다. 상세는 work_log.md 참고.
package com.logiccheck.review.support;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 애플리케이션 클래스가 com.example.business 에 있어 com.logiccheck 하위 엔티티·리포지토리가
 * 자동 탐색되지 않는다. 개발자1이 뼈대를 com.logiccheck 로 옮기면 이 파일은 필요 없다.
 */
@Configuration
@EntityScan(basePackages = "com.logiccheck")
@EnableJpaRepositories(basePackages = "com.logiccheck")
public class ReviewPersistenceConfig {
}
