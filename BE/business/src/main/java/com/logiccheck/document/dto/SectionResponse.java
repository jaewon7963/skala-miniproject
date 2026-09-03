package com.logiccheck.document.dto;

import java.util.List;

import com.logiccheck.document.entity.Section;

public record SectionResponse(String id, String title, int level, Integer pageFrom, Integer pageTo, int orderNo,
                               String source, List<SectionResponse> children) {

    public static SectionResponse withChildren(Section section, List<SectionResponse> children) {
        return new SectionResponse(String.valueOf(section.getId()), section.getTitle(), section.getLevel(),
                section.getPageFrom(), section.getPageTo(), section.getOrderNo(), section.getSource().name(),
                children);
    }
}
