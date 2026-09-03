package com.logiccheck.review;

import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 명세 1-2: 서버는 UTC 로 저장하고 응답은 +09:00 오프셋을 포함한 ISO-8601 로 내린다.
 * 응답 DTO 를 만들 때 저장된 UTC 값을 이 메서드로 변환한다.
 */
public final class ReviewTimes {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private ReviewTimes() {
    }

    public static OffsetDateTime toResponse(OffsetDateTime utc) {
        return utc == null ? null : utc.atZoneSameInstant(KST).toOffsetDateTime();
    }
}
