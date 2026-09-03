package com.logiccheck.review.job;

import java.util.List;

import com.logiccheck.document.entity.PageBlock;

/**
 * 원문 뷰어가 한 페이지를 그리는 데 필요한 전부.
 *
 * <p>내용이 없는 페이지도 빠뜨리지 않고 내려보낸다. 뷰어가 배열 길이를 전체 쪽수로 쓰기 때문에
 * 중간을 건너뛰면 페이지 번호가 어긋난다.
 */
public record DocumentPageResponse(int page, String sectionId, String sectionTitle, List<PageBlock> blocks) {
}
