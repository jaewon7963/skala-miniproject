package com.logiccheck.document.dto;

import java.util.List;

public record DocumentUpdateRequest(String title, List<String> tagIds) {
}
