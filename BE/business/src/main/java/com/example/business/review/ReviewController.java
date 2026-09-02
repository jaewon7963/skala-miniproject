package com.example.business.review;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ReviewController {
    private final Map<String, Finding> findings = new ConcurrentHashMap<>();
    private final Map<String, List<AuditEvent>> history = new ConcurrentHashMap<>();

    public ReviewController() {
        seed("f-101", "numeric_conflict", "high", "2027년 예상 매출이 서로 다릅니다",
                "시장 진입 계획은 18억 원, 매출 계획은 24억 원으로 작성되어 있습니다.",
                List.of(new Evidence(11, "2027년 예상 매출 18억 원", 42), new Evidence(16, "2027년 매출 목표 24억 원", 68)));
        seed("f-102", "calculation", "high", "인건비 합계가 산출식과 맞지 않습니다",
                "인원 × 참여기간 × 월 단가로 재계산한 값이 기재 금액보다 2,400만 원 적습니다.",
                List.of(new Evidence(14, "5명 × 8개월 × 월 450만 원", 36), new Evidence(17, "총 인건비 2억 400만 원", 51)));
        seed("f-103", "tech_kpi", "medium", "실시간 분석 목표와 기술 구성을 확인해야 합니다",
                "0.5초 응답 목표에 비해 대형 비전 모델 3종을 순차 실행하며 처리량 근거가 없습니다.",
                List.of(new Evidence(8, "대형 비전 모델 3종 순차 처리", 45), new Evidence(19, "평균 응답시간 0.5초 이내", 61)));
        seed("f-104", "evidence_missing", "medium", "정확도 KPI의 평가 방법이 없습니다",
                "정확도 95%의 시험 데이터 규모와 측정 지표가 정의되지 않았습니다.",
                List.of(new Evidence(19, "객체 인식 정확도 95%", 30)));
        seed("f-105", "business_conflict", "low", "목표 고객과 판매 채널이 일치하지 않습니다",
                "최종 사용자는 개인 고객이지만 판매 계획은 기업 직접 영업만 제시합니다.",
                List.of(new Evidence(5, "목표 고객: 20~30대 개인 사용자", 25), new Evidence(12, "대기업 구매팀 직접 영업", 72)));
    }

    private void seed(String id, String type, String severity, String title, String description, List<Evidence> evidence) {
        findings.put(id, new Finding(id, type, severity, "unreviewed", title, description, evidence, 0.94));
        history.put(id, new ArrayList<>());
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("ok", true, "service", "logiccheck-api");
    }

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@RequestParam MultipartFile file) {
        String name = file.getOriginalFilename();
        if (file.isEmpty() || name == null || !name.toLowerCase().endsWith(".pdf")) {
            throw new InvalidReviewRequestException("PDF 파일만 업로드할 수 있습니다.");
        }
        return new DocumentResponse("demo", name, "completed", 21, findings.size());
    }

    @GetMapping("/documents/demo")
    public DocumentResponse document() {
        return new DocumentResponse("demo", "AI 매장 안내 로봇 사업계획서.pdf", "completed", 21, findings.size());
    }

    @GetMapping("/documents/demo/findings")
    public List<Finding> list(@RequestParam(required = false) String status) {
        return findings.values().stream()
                .filter(finding -> status == null || status.equals(finding.status()))
                .sorted((left, right) -> left.id().compareTo(right.id()))
                .toList();
    }

    @GetMapping("/findings/{id}")
    public Finding detail(@PathVariable String id) {
        return requiredFinding(id);
    }

    @PatchMapping("/findings/{id}/decision")
    public Finding decide(@PathVariable String id, @RequestBody DecisionRequest request) {
        Finding before = requiredFinding(id);
        if (!List.of("confirmed", "dismissed", "pending", "revision_requested").contains(request.status())) {
            throw new InvalidReviewRequestException("지원하지 않는 검토 상태입니다.");
        }
        Finding after = new Finding(before.id(), before.type(), before.severity(), request.status(), before.title(),
                before.description(), before.evidence(), before.confidence());
        findings.put(id, after);
        history.get(id).add(new AuditEvent("김대현", "decision.changed", before.status(), after.status(),
                request.comment(), OffsetDateTime.now()));
        return after;
    }

    @GetMapping("/findings/{id}/history")
    public List<AuditEvent> history(@PathVariable String id) {
        requiredFinding(id);
        return List.copyOf(history.get(id));
    }

    private Finding requiredFinding(String id) {
        Finding finding = findings.get(id);
        if (finding == null) throw new FindingNotFoundException();
        return finding;
    }

    public record DocumentResponse(String id, String name, String status, int pageCount, int findingCount) {}
    public record Finding(String id, String type, String severity, String status, String title,
                          String description, List<Evidence> evidence, double confidence) {}
    public record Evidence(int page, String text, int topPercent) {}
    public record DecisionRequest(String status, String comment) {}
    public record AuditEvent(String actor, String action, String before, String after, String comment,
                             OffsetDateTime createdAt) {}

    @ResponseStatus(HttpStatus.NOT_FOUND)
    private static class FindingNotFoundException extends RuntimeException {}

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    private static class InvalidReviewRequestException extends RuntimeException {
        InvalidReviewRequestException(String message) { super(message); }
    }
}
