package com.logiccheck.review.job;

/**
 * 진행 화면 좌측에 순서대로 표시되는 파이프라인 단계.
 *
 * <p>{@code state}는 {@code WAIT} · {@code RUNNING} · {@code DONE} 중 하나이며
 * 화면이 값을 그대로 소문자로 바꿔 CSS 클래스로 쓰기 때문에 null이 되면 안 된다.
 */
public record JobStep(String key, String label, String detail, String state) {

    public static final String WAIT = "WAIT";
    public static final String RUNNING = "RUNNING";
    public static final String DONE = "DONE";

    public JobStep waiting() {
        return new JobStep(key, label, detail, WAIT);
    }

    public JobStep running() {
        return new JobStep(key, label, detail, RUNNING);
    }

    public JobStep done() {
        return new JobStep(key, label, detail, DONE);
    }
}
