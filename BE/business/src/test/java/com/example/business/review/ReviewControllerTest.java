package com.example.business.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ReviewControllerTest {
    @Test
    void demoContainsReviewFindings() {
        ReviewController controller = new ReviewController();
        assertThat(controller.list(null)).hasSize(5);
        assertThat(controller.history("f-101")).isEmpty();
    }

    @Test
    void rejectsNonPdfUpload() {
        ReviewController controller = new ReviewController();
        MockMultipartFile file = new MockMultipartFile("file", "plan.txt", "text/plain", "draft".getBytes());

        assertThatThrownBy(() -> controller.upload(file))
                .hasMessage("PDF 파일만 업로드할 수 있습니다.");
    }
}
