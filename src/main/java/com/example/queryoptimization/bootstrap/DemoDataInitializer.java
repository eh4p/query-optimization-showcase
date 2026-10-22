package com.example.queryoptimization.bootstrap;

import com.example.queryoptimization.entity.Currency;
import com.example.queryoptimization.entity.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Order(0)
public class DemoDataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final int ACCOUNT_COUNT = 50;
    private static final int MERCHANT_COUNT = 100;
    private static final int TRANSACTION_COUNT = 50_000;
    private static final int BATCH_SIZE = 1_000;
    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final String[] MERCHANT_NAMES = {
            "Demo Market", "Sample Electronics", "Fake Coffee Shop", "Example Telecom"
    };
    private static final String[] CATEGORIES = {"GROCERIES", "ELECTRONICS", "COFFEE", "TELECOM"};
    private final JdbcTemplate jdbc;

    public DemoDataInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Object[]> accounts = new ArrayList<>();
        for (int id = 1; id <= ACCOUNT_COUNT; id++) {
            accounts.add(new Object[]{id, accountNumber(id), "Demo Customer " + id});
        }
        jdbc.batchUpdate("INSERT INTO accounts VALUES (?, ?, ?)", accounts);
        List<Object[]> merchants = new ArrayList<>();
        for (int id = 1; id <= MERCHANT_COUNT; id++) {
            int category = (id - 1) % MERCHANT_NAMES.length;
            merchants.add(new Object[]{id, MERCHANT_NAMES[category] + " " + id, CATEGORIES[category]});
        }
        jdbc.batchUpdate("INSERT INTO merchants VALUES (?, ?, ?)", merchants);

        Currency[] currencies = Currency.values();
        TransactionStatus[] statuses = TransactionStatus.values();
        for (int first = 1; first <= TRANSACTION_COUNT; first += BATCH_SIZE) {
            List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
            for (int id = first; id < first + BATCH_SIZE && id <= TRANSACTION_COUNT; id++) {
                int ordinal = id - 1;
                int account = ordinal % ACCOUNT_COUNT + 1;
                int merchant = (ordinal / ACCOUNT_COUNT + account * 7) % MERCHANT_COUNT + 1;
                // Five rows share a timestamp: cursor tie-breaking is exercised by real data.
                LocalDateTime createdAt = START.plusSeconds((ordinal / 5L) * 3_153);
                batch.add(new Object[]{id, accountNumber(account), merchant,
                        BigDecimal.valueOf(100 + (ordinal * 37L) % 100_000, 2),
                        currencies[ordinal % currencies.length].name(),
                        statuses[(ordinal / 7) % statuses.length].name(), createdAt,
                        "Synthetic transaction " + id});
            }
            jdbc.batchUpdate("""
                    INSERT INTO transactions
                        (id, account_id, merchant_id, amount, currency, status, created_at, description)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, batch);
        }
        jdbc.update("INSERT INTO transactions_unindexed SELECT * FROM transactions");
        jdbc.execute("ANALYZE");
        log.info("Seeded {} fake accounts, {} merchants, {} transactions + {} unindexed copies",
                ACCOUNT_COUNT, MERCHANT_COUNT, TRANSACTION_COUNT, TRANSACTION_COUNT);
    }

    private static String accountNumber(int id) {
        return String.format(Locale.ROOT, "ACC-%05d", id);
    }
}
