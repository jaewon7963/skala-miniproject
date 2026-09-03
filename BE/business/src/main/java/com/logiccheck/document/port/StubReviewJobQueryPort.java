package com.logiccheck.document.port;

import java.util.Collection;
import java.util.Map;

import org.springframework.stereotype.Component;

// ponytail: review 도메인이 아직 없어 "진행 중인 분석 작업 없음"으로 고정 응답한다.
// review 도메인의 실제 구현이 merge되면 이 클래스를 삭제한다(비활성화 아님) —
// 같은 인터페이스에 빈이 두 개면 기동이 깨진다.
@Component
public class StubReviewJobQueryPort implements ReviewJobQueryPort {

    @Override
    public Map<Long, LatestJobView> findLatestByDocumentIds(Collection<Long> documentIds) {
        return Map.of();
    }
}
