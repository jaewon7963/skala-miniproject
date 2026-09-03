package com.logiccheck.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 돌려줄 리소스가 없는 성공 응답. 탈퇴에만 유예 기간이 함께 붙는다. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OkResponse(boolean ok, Integer purgeAfterDays) {

    public static OkResponse success() {
        return new OkResponse(true, null);
    }

    public static OkResponse withdrawn(int purgeAfterDays) {
        return new OkResponse(true, purgeAfterDays);
    }
}
