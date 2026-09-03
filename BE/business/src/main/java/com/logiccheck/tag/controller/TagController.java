package com.logiccheck.tag.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logiccheck.tag.dto.TagResponse;
import com.logiccheck.tag.repository.TagRepository;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagRepository tagRepository;

    public TagController(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @GetMapping
    public List<TagResponse> list() {
        return tagRepository.findAllByOrderByOrderNoAsc().stream()
                .map(TagResponse::from)
                .toList();
    }
}
