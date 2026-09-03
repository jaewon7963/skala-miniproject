package com.logiccheck.review.job;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 진행 화면에 보여줄 파이프라인 단계 정의.
 *
 * <p>라벨과 설명은 서버가 내려준다. 실제로 하는 일과 문구가 어긋나면 사용자가
 * 진행 상황을 오해하므로, 단계를 바꾸면 이 문구도 같이 고친다.
 *
 * <p>작업마다 저장하는 것은 <b>단계별 상태뿐</b>이다. 아래 이름과 문구는 모든 작업이
 * 똑같이 쓰는 값이라 행마다 복사할 이유가 없고, 응답을 만들 때 여기서 붙인다.
 */
public final class ReviewJobSteps {

    private record Definition(String key, String label, String detail) {
    }

    private static final List<Definition> TEMPLATE = List.of(
            new Definition("validate", "파일 검증", "형식과 용량을 확인합니다"),
            new Definition("split", "페이지 분할 · 텍스트 레이어 추출", "페이지마다 본문을 읽어냅니다"),
            new Definition("outline", "목차 · 섹션 인식", "제목 계층을 복원합니다"),
            new Definition("extract", "표 · 수치 추출", "문단과 표를 나눠 위치를 붙입니다"),
            new Definition("persist", "구조화 저장", "검토 화면이 참조할 형태로 저장합니다"));

    private ReviewJobSteps() {
    }

    public static int count() {
        return TEMPLATE.size();
    }

    public static List<String> waiting() {
        return TEMPLATE.stream().map(definition -> JobStep.WAIT).toList();
    }

    /** {@code index}번째 단계가 진행 중이고 그 앞은 모두 끝난 상태. */
    public static List<String> running(int index) {
        return build(index, false);
    }

    /** {@code index}번째까지 끝난 상태. */
    public static List<String> completedThrough(int index) {
        return build(index, true);
    }

    public static List<String> allDone() {
        return completedThrough(TEMPLATE.size() - 1);
    }

    /**
     * 저장된 상태에 이름과 문구를 붙여 화면이 쓰는 모양으로 만든다.
     * 상태가 모자라거나 비어 있어도 대기 상태로 채운다 — 화면이 상태를 그대로
     * 소문자로 바꿔 CSS 클래스에 쓰기 때문에 null이 섞이면 그 자리에서 죽는다.
     */
    public static List<JobStep> describe(List<String> states) {
        return IntStream.range(0, TEMPLATE.size())
                .mapToObj(i -> {
                    Definition definition = TEMPLATE.get(i);
                    String state = states != null && i < states.size() && states.get(i) != null
                            ? states.get(i)
                            : JobStep.WAIT;
                    return new JobStep(definition.key(), definition.label(), definition.detail(), state);
                })
                .toList();
    }

    private static List<String> build(int index, boolean includeCurrent) {
        return IntStream.range(0, TEMPLATE.size())
                .mapToObj(i -> {
                    if (i < index || (i == index && includeCurrent)) {
                        return JobStep.DONE;
                    }
                    return i == index ? JobStep.RUNNING : JobStep.WAIT;
                })
                .toList();
    }
}
