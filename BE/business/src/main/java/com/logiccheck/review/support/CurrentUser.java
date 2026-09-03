// TEMP: 개발자1(global/) 산출물 머지 시 이 파일을 삭제하고 import 를 교체한다. 상세는 work_log.md 참고.
package com.logiccheck.review.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
