package com.logiccheck.document.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

// ponytail: 미니프로젝트 규모에 맞춰 로컬 파일시스템만 지원한다.
// 실제 배포 대상이 생기면 인터페이스로 뽑아 S3/MinIO 구현을 추가한다.
@Component
public class LocalFileStorageService {

    private final Path baseDir;

    public LocalFileStorageService(@Value("${document.storage.base-dir}") String baseDir) {
        this.baseDir = Path.of(baseDir);
        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String store(InputStream content) {
        LocalDate today = LocalDate.now();
        String relativeKey = "%d/%02d/%02d/%s.pdf".formatted(
                today.getYear(), today.getMonthValue(), today.getDayOfMonth(), UUID.randomUUID());
        Path target = baseDir.resolve(relativeKey);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return relativeKey;
    }

    public Resource load(String fileKey) {
        return new FileSystemResource(baseDir.resolve(fileKey));
    }

    public Path resolve(String fileKey) {
        return baseDir.resolve(fileKey);
    }
}
