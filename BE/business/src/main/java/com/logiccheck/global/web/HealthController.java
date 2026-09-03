package com.logiccheck.global.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 컨테이너와 배포 스크립트가 기동 여부를 확인하는 용도. */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("ok", true, "service", "bizxray-api");
    }
}
