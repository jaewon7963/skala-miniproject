package com.logiccheck.review.job;

/**
 * 진행 화면 좌측에 순서대로 표시되는 파이프라인 단계. 응답에만 쓰는 표현이다.
 *
 * <p>{@code key} · {@code label} · {@code detail} 은 {@link ReviewJobSteps} 의 고정값이고,
 * 저장되는 것은 {@code state} 뿐이다. {@code state} 는 {@code WAIT} · {@code RUNNING} ·
 * {@code DONE} 중 하나이며, 화면이 값을 그대로 소문자로 바꿔 CSS 클래스로 쓰기 때문에
 * null이 되면 안 된다.
 */
public record JobStep(String key, String label, String detail, String state) {

    public static final String WAIT = "WAIT";
    public static final String RUNNING = "RUNNING";
    public static final String DONE = "DONE";
}
