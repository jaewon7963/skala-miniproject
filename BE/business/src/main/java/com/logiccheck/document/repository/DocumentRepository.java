package com.logiccheck.document.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.logiccheck.document.entity.Document;
import com.logiccheck.document.entity.ParseStatus;

public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    Optional<Document> findByIdAndOwnerIdAndDeletedAtIsNull(Long id, Long ownerId);

    boolean existsByIdAndDeletedAtIsNull(Long id);

    boolean existsByOwnerIdAndFileHashAndDeletedAtIsNull(Long ownerId, String fileHash);

    List<DocIdStatus> findByOwnerIdAndDeletedAtIsNull(Long ownerId);

    interface DocIdStatus {
        Long getId();

        ParseStatus getParseStatus();
    }
}
