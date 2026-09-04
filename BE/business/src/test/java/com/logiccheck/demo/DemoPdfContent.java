package com.logiccheck.demo;

import java.util.List;

/**
 * 시연용 사업계획서의 본문. 문장 하나하나가 파서와 검토 규칙을 겨냥해 쓰여 있다.
 *
 * <p>이 문서는 "그럴듯한 사업계획서"인 동시에 {@code StubReviewAiClient}의 네 가지 규칙을
 * 모두 건드리도록 설계한 시연 대본이다. 아무 PDF나 넣으면 규칙에 걸리는 게 없어
 * 보완 규칙({@code reviewWorthyParagraphs})만 발동하고 "분량이 큰 문단입니다" 세 건으로 끝난다.
 *
 * <p>어느 문장이 무엇을 유발하는지는 {@code DemoPdfFindingsTest}가 그대로 검증한다.
 * 본문을 고치면 그 테스트가 먼저 깨지도록 해 두었다.
 */
final class DemoPdfContent {

    static final String TITLE = "AI 매장 안내 로봇 사업계획서";

    private DemoPdfContent() {
    }

    /** 표 1의 열 너비(pt). 마지막 열은 남는 자리를 쓴다. */
    private static final float[] REVENUE_COLUMNS = {120, 105, 105, 105};
    private static final float[] KPI_COLUMNS = {110, 100, 100, 100};

    static List<Page> pages() {
        return List.of(
                page("1. 사업 개요", 1, List.of(
                        Line.heading(TITLE),
                        Line.heading("주식회사 로직체크 · 2026년 3월"),
                        Line.blank(),
                        Line.heading("1. 사업 개요"),
                        Line.body("본 사업은 대형 유통 매장에 자율주행 안내 로봇을 배치하여 고객 응대 인력을"),
                        Line.body("보조하고 매장 운영 데이터를 모으는 것을 목표로 한다. 로봇은 상품의 위치를"),
                        Line.body("안내하고 재고를 조회하며 다국어 응대까지 수행하고, 매장 관리 시스템과 실시간으로"),
                        Line.body("연동된다. 1차 연도에는 수도권 120개 매장에 우선 배치하여 운영 데이터를"),
                        Line.body("쌓고 응대 품질을 다듬는다."),
                        Line.blank(),
                        Line.body("사업 기간은 2026년 3월부터 2029년 2월까지 3년이며, 1차 연도는"),
                        Line.body("시범 운영, 2차 연도는 확산, 3차 연도는 전국 전개로 구분한다."))),

                page("2. 시장 분석", 2, List.of(
                        Line.heading("2. 시장 분석"),
                        Line.body("국내 서비스 로봇 시장은 유통과 물류를 중심으로 빠르게 커지고 있으며,"),
                        Line.body("2028년 국내 매장용 서비스 로봇 시장 규모는 1조 2천억 원에 이를 것으로"),
                        Line.body("추정된다. 대형 유통사의 무인화 투자가 이어지면서 매장 안내 로봇의 도입 속도도"),
                        Line.body("함께 빨라질 것으로 전망된다."),
                        Line.blank(),
                        Line.body("무인 계산대가 이미 깔린 매장은 로봇을 붙일 때 추가 공사가 거의 없어,"),
                        Line.body("초기 도입 부담이 낮은 곳부터 순서대로 공략한다."))),

                // 같은 주장을 하면서 출처를 밝힌 문단. 규칙이 무차별로 걸지 않는다는 것을 보여주는 대조군이라
                // 절대로 지적이 붙으면 안 된다.
                page("2.1 참고 자료", 3, List.of(
                        Line.heading("2.1 참고 자료"),
                        Line.body("한국로봇산업진흥원이 펴낸 2025년 로봇산업 실태조사 보고서를 참조하여 국내"),
                        Line.body("산업 구조와 도입 현황을 정리하였다. 조사 대상은 국내 로봇 기업 412곳이며,"),
                        Line.body("표본과 조사 시점은 아래 자료에 적힌 그대로 인용하였다."),
                        Line.body("출처: 한국로봇산업진흥원, 2025 로봇산업 실태조사."))),

                page("3. 성장 시나리오", 4, List.of(
                        Line.heading("3. 성장 시나리오"),
                        Line.body("매장용 안내 로봇 시장은 2026년부터 2030년까지 연평균 성장률 32%로"),
                        Line.body("커질 것으로 본다. 같은 기간 당사 매출은 연평균 41% 늘어 시장 전체의"),
                        Line.body("확대 속도를 웃도는 흐름을 이어갈 것으로 본다."))),

                page("3.1 경쟁 구도", 5, List.of(
                        Line.heading("3.1 경쟁 구도"),
                        Line.body("현재 국내에서 매장 안내 로봇을 공급하는 곳은 대기업 계열 두 곳과 초기 단계"),
                        Line.body("스타트업 여섯 곳이다. 대부분 하드웨어 판매에 머물러 있어, 당사는 운영을"),
                        Line.body("떠받치는 관제 소프트웨어를 함께 제공해 차별화한다."),
                        Line.blank(),
                        Line.body("경쟁사가 따라오기 어려운 지점은 매장별 동선 데이터를 계속 쌓아 응대 품질을"),
                        Line.body("다듬어 가는 구조라고 본다."))),

                page("4. 매출 계획", 6, List.of(
                        Line.heading("4. 매출 계획"),
                        Line.caption("[표 1] 연도별 매출 계획 (단위: 백만원)"),
                        Line.row(REVENUE_COLUMNS, "구분", "2026년", "2027년", "2028년"),
                        Line.row(REVENUE_COLUMNS, "제품 매출", "1,200", "2,400", "4,800"),
                        Line.row(REVENUE_COLUMNS, "서비스 매출", "300", "700", "1,500"),
                        Line.row(REVENUE_COLUMNS, "합계", "1,500", "3,100", "7,000"),
                        Line.blank(),
                        Line.body("제품 매출은 로봇 본체 판매에서, 서비스 매출은 월 구독형 관제 서비스에서"),
                        Line.body("발생한다. 구독 요금은 로봇 한 대당 월 12만원으로 잡았다."))),

                page("5. 확대 계획", 7, List.of(
                        Line.heading("5. 확대 계획"),
                        Line.body("2차 연도에는 광역시를 중심으로 서비스 지역을 넓히고, 3차 연도까지 전국"),
                        Line.body("150개 매장으로 확대하여 안정적인 유지보수 매출 구조를 만든다."),
                        Line.body("권역별 서비스 거점을 두어 장애 대응 시간을 줄이고 로봇 가동률을 함께 끌어올린다."),
                        Line.blank(),
                        Line.body("확대 단계에서는 신규 인력을 매장 수에 맞춰 늘리기보다 원격 관제 비중을 높여"),
                        Line.body("고정비 증가를 억제하는 방향으로 운영한다."))),

                page("6. 핵심 성과지표", 8, List.of(
                        Line.heading("6. 핵심 성과지표"),
                        Line.caption("[표 2] 핵심 성과지표(KPI) 목표"),
                        Line.row(KPI_COLUMNS, "지표", "2026년 목표", "측정 주기", "산식"),
                        Line.row(KPI_COLUMNS, "응대 정확도", "92%", "월간", "-"),
                        Line.row(KPI_COLUMNS, "재방문율", "35%", "-", "-"),
                        Line.row(KPI_COLUMNS, "평균 응대 시간", "25초", "월간", "-"),
                        Line.blank(),
                        Line.body("목표치는 1차 연도 말 시점의 값이며, 미달할 경우 원인을 따져 개선 계획을"),
                        Line.body("다시 세운다."))));
    }

    private static Page page(String bookmark, int pageNo, List<Line> lines) {
        return new Page(bookmark, pageNo, lines);
    }

    record Page(String bookmark, int pageNo, List<Line> lines) {
    }

    /** 한 줄. {@code kind}에 따라 파서가 제목·본문·표 행 중 무엇으로 읽을지가 갈린다. */
    record Line(Kind kind, String text, List<String> cells, float[] columns) {

        enum Kind { HEADING, CAPTION, BODY, ROW, BLANK }

        static Line heading(String text) {
            return new Line(Kind.HEADING, text, null, null);
        }

        /** 표 바로 앞의 "[표 N] ..." 줄. 파서가 제목으로 읽었다가 표 캡션으로 옮겨 붙인다. */
        static Line caption(String text) {
            return new Line(Kind.CAPTION, text, null, null);
        }

        static Line body(String text) {
            return new Line(Kind.BODY, text, null, null);
        }

        static Line row(float[] columns, String... cells) {
            return new Line(Kind.ROW, null, List.of(cells), columns);
        }

        static Line blank() {
            return new Line(Kind.BLANK, "", null, null);
        }
    }
}
