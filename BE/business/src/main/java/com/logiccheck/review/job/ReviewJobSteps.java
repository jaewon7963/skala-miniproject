package com.logiccheck.review.job;

import java.util.List;

/**
 * 진행 화면에 보여줄 파이프라인 단계 정의.
 *
 * <p>라벨과 설명은 서버가 내려준다. 실제로 하는 일과 문구가 어긋나면 사용자가
 * 진행 상황을 오해하므로, 단계를 바꾸면 이 문구도 같이 고친다.
 */
public final class ReviewJobSteps {

    private static final List<JobStep> TEMPLATE = List.of(
            new JobStep("validate", "파일 검증", "형식과 용량을 확인합니다", JobStep.WAIT),
            new JobStep("split", "페이지 분할 · 텍스트 레이어 추출", "페이지마다 본문을 읽어냅니다", JobStep.WAIT),
            new JobStep("outline", "목차 · 섹션 인식", "제목 계층을 복원합니다", JobStep.WAIT),
            new JobStep("extract", "표 · 수치 추출", "문단과 표를 나눠 위치를 붙입니다", JobStep.WAIT),
            new JobStep("persist", "구조화 저장", "검토 화면이 참조할 형태로 저장합니다", JobStep.WAIT));

    private ReviewJobSteps() {
    }

    public static int count() {
        return TEMPLATE.size();
    }

    public static List<JobStep> waiting() {
        return TEMPLATE;
    }

    /** {@code index}번째 단계가 진행 중이고 그 앞은 모두 끝난 상태. */
    public static List<JobStep> running(int index) {
        return build(index, false);
    }

    /** {@code index}번째까지 끝난 상태. */
    public static List<JobStep> completedThrough(int index) {
        return build(index, true);
    }

    public static List<JobStep> allDone() {
        return completedThrough(TEMPLATE.size() - 1);
    }

    private static List<JobStep> build(int index, boolean includeCurrent) {
        return java.util.stream.IntStream.range(0, TEMPLATE.size())
                .mapToObj(i -> {
                    JobStep step = TEMPLATE.get(i);
                    if (i < index || (i == index && includeCurrent)) {
                        return step.done();
                    }
                    return i == index ? step.running() : step.waiting();
                })
                .toList();
    }
}
