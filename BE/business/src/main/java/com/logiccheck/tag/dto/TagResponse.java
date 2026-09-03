package com.logiccheck.tag.dto;

import com.logiccheck.tag.entity.Tag;

public record TagResponse(String id, String code, String name) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(String.valueOf(tag.getId()), tag.getCode(), tag.getName());
    }
}
