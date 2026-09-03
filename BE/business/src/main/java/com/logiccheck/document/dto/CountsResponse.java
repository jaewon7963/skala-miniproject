package com.logiccheck.document.dto;

/**
 * 좌측 사이드바 배지에 쓰는 상태별 문서 수.
 *
 * <p>검색·필터와 무관하게 항상 소유자 전체를 기준으로 센다. 필터를 걸어도
 * 배지 숫자가 흔들리지 않아야 하기 때문이다. 화면이 키를 직접 찾아 쓰므로
 * 상태 하나라도 빠지면 그 배지가 0으로 보인다.
 */
public record CountsResponse(long ALL, long IDLE, long PARSING, long ANALYZING, long REVIEWING, long DONE,
                              long FAILED) {
}
