// TEMP: 개발자1의 global/config/AsyncConfig 가 머지되면 이 파일을 삭제한다.
package com.logiccheck.review.support;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** 명세 16 의 파이프라인을 별도 스레드에서 돌리기 위해 필요하다. */
@Configuration
@EnableAsync
public class ReviewAsyncConfig {
}
