package com.logiccheck.review.job;

/** 질문 패널 입력. {@code selection}은 원문에서 잡아 놓은 구절이며 없을 수 있다. */
public record QuestionRequest(String question, Selection selection) {

    public record Selection(Integer page, String text, String quote) {
    }

    public String selectionText() {
        if (selection == null) {
            return null;
        }
        return selection.text() != null ? selection.text() : selection.quote();
    }
}
