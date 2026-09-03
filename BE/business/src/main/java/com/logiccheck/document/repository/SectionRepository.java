package com.logiccheck.document.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.logiccheck.document.entity.Section;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByDocument_IdOrderByOrderNoAsc(Long documentId);
}
