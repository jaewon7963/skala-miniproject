// TEMP: 개발자2의 문서 파싱과 S5 파이프라인이 붙으면 필요 없다. seed 프로파일에서만 동작한다.
package com.logiccheck.review.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * AI 파이프라인 없이 명세 21 · 22 를 검증하기 위한 시드 실행기 (DEV3 C-3 7단계).
 * 실행: --spring.profiles.active=stub,seed
 */
@Profile("seed")
@Component
public class ReviewSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReviewSeedRunner.class);

    private final DataSource dataSource;

    public ReviewSeedRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/seed/review_seed.sql"));
        }
        log.info("검토사항 시드를 적용했다 (document_id = 900001)");
    }
}
