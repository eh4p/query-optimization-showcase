package com.example.queryoptimization.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

@Component
@Profile("diagnostics")
@Order(1)
public class AnalysisPlanReporter implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AnalysisPlanReporter.class);
    private final JdbcTemplate jdbc;
    private final ApplicationContext context;

    public AnalysisPlanReporter(JdbcTemplate jdbc, ApplicationContext context) {
        this.jdbc = jdbc;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Resource[] scripts = context.getResources("classpath*:sql/analysis/*.sql");
        Arrays.sort(scripts, Comparator.comparing(Resource::getFilename));
        for (Resource script : scripts) {
            String sql = script.getContentAsString(StandardCharsets.UTF_8);
            String plan = jdbc.queryForObject(sql, String.class);
            log.info("Execution plan [{}]:\n{}", script.getFilename(), plan);
        }
    }
}
