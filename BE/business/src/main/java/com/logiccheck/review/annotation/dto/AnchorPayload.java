package com.logiccheck.review.annotation.dto;

import com.logiccheck.review.annotation.Annotation.Anchor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** bbox 는 페이지 크기 대비 0~1 상대 좌표다 (DEV3 D-5). */
public record AnchorPayload(
        @Positive Integer page,
        String quote,
        @Valid BBoxPayload bbox
) {

    public Anchor toAnchor() {
        BBoxPayload box = bbox;
        return new Anchor(page, quote,
                box == null ? null : box.x(),
                box == null ? null : box.y(),
                box == null ? null : box.w(),
                box == null ? null : box.h());
    }

    /** 네 값이 전부 있어야 한다. 부분 좌표는 DB CHECK 제약(ck_annotations_bbox)에서도 거부된다. */
    public record BBoxPayload(
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal x,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal y,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal w,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal h
    ) {
    }
}
