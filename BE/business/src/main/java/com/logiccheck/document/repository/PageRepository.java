package com.logiccheck.document.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.logiccheck.document.entity.Page;

public interface PageRepository extends JpaRepository<Page, Long> {

    List<Page> findByDocument_IdOrderByPageNoAsc(Long documentId);
}
