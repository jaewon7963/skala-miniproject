package com.logiccheck.tag.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.logiccheck.tag.entity.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findAllByOrderByOrderNoAsc();
}
