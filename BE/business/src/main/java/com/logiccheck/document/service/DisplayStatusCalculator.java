package com.logiccheck.document.service;

import com.logiccheck.document.entity.ParseStatus;
import com.logiccheck.document.port.ReviewJobQueryPort.LatestJobView;

public final class DisplayStatusCalculator {

    private DisplayStatusCalculator() {
    }

    public static String calculate(ParseStatus parseStatus, LatestJobView job) {
        if (parseStatus != ParseStatus.DONE) {
            return switch (parseStatus) {
                case PENDING -> "PARSE_PENDING";
                case PARSING -> "PARSING";
                case EXTRACTING -> "EXTRACTING";
                case FAILED -> "PARSE_FAILED";
                case DONE -> throw new IllegalStateException("unreachable");
            };
        }
        if (job == null) {
            return "IDLE";
        }
        return switch (job.status()) {
            case "PENDING", "RUNNING" -> "ANALYZING";
            case "FAILED" -> "ANALYSIS_FAILED";
            default -> "COMPLETED".equals(job.reviewStatus()) ? "COMPLETED" : "IN_REVIEW";
        };
    }

    public static boolean isFailed(String displayStatus) {
        return "PARSE_FAILED".equals(displayStatus) || "ANALYSIS_FAILED".equals(displayStatus);
    }
}
