package com.logiccheck.document.dto;

public record CountsResponse(long ALL, long IN_REVIEW, long COMPLETED, long FAILED) {
}
