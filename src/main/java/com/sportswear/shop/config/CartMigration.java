package com.sportswear.shop.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Drops the old unique constraint (user_id, product_id) on cart_items
 * so that the same product with different sizes can be stored as separate rows.
 */
@Component
public class CartMigration {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateCartItemConstraint() {
        // 1. Change cart_items.size from INT to VARCHAR if needed
        try {
            String colType = jdbcTemplate.queryForObject(
                "SELECT DATA_TYPE FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cart_items' AND COLUMN_NAME = 'size'",
                String.class);
            if (colType != null && !colType.toLowerCase().startsWith("var")) {
                jdbcTemplate.execute("ALTER TABLE cart_items MODIFY COLUMN size VARCHAR(10)");
            }
        } catch (Exception ignored) {}

        // 2. Change order_details.size from INT to VARCHAR if needed
        try {
            String colType = jdbcTemplate.queryForObject(
                "SELECT DATA_TYPE FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_details' AND COLUMN_NAME = 'size'",
                String.class);
            if (colType != null && !colType.toLowerCase().startsWith("var")) {
                jdbcTemplate.execute("ALTER TABLE order_details MODIFY COLUMN size VARCHAR(10)");
            }
        } catch (Exception ignored) {}

        // 3. Change product_sizes.sizes from INT to VARCHAR if needed
        try {
            String colType = jdbcTemplate.queryForObject(
                "SELECT DATA_TYPE FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_sizes' AND COLUMN_NAME = 'sizes'",
                String.class);
            if (colType != null && !colType.toLowerCase().startsWith("var")) {
                jdbcTemplate.execute("ALTER TABLE product_sizes MODIFY COLUMN sizes VARCHAR(10)");
            }
        } catch (Exception ignored) {}

        // 4. Drop old unique constraint on cart_items that doesn't include 'size'
        try {
            List<String> toRemove = jdbcTemplate.queryForList(
                "SELECT DISTINCT k.CONSTRAINT_NAME " +
                "FROM information_schema.KEY_COLUMN_USAGE k " +
                "JOIN information_schema.TABLE_CONSTRAINTS t " +
                "  ON k.CONSTRAINT_NAME = t.CONSTRAINT_NAME " +
                "  AND k.TABLE_NAME = t.TABLE_NAME " +
                "  AND k.TABLE_SCHEMA = t.TABLE_SCHEMA " +
                "WHERE k.TABLE_SCHEMA = DATABASE() " +
                "  AND k.TABLE_NAME = 'cart_items' " +
                "  AND t.CONSTRAINT_TYPE = 'UNIQUE' " +
                "  AND k.CONSTRAINT_NAME NOT IN (" +
                "    SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                "    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cart_items' AND COLUMN_NAME = 'size'" +
                "  ) AND k.CONSTRAINT_NAME != 'PRIMARY'",
                String.class
            );
            for (String name : toRemove) {
                jdbcTemplate.execute("ALTER TABLE cart_items DROP INDEX `" + name + "`");
            }
        } catch (Exception ignored) {}
    }
}
