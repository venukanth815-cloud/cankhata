package com.cankhata.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Runs schema.sql at startup. Every statement is IF NOT EXISTS / INSERT IGNORE, so repeating it is harmless. */
public final class SchemaInitializer {
    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    private SchemaInitializer() {}

    public static void run() {
        try (InputStream in = SchemaInitializer.class.getResourceAsStream("/schema.sql")) {
            if (in == null) throw new IllegalStateException("schema.sql not found on classpath");
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            StringBuilder clean = new StringBuilder();
            for (String line : sql.split("\n")) {
                String t = line.trim();
                if (t.startsWith("--")) continue;               // whole-line comments
                int c = line.indexOf("--");                     // trailing comments (no '--' inside our strings)
                clean.append(c >= 0 ? line.substring(0, c) : line).append('\n');
            }
            int count = 0;
            try (Connection con = DbConnection.get(); Statement st = con.createStatement()) {
                for (String stmt : clean.toString().split(";")) {
                    if (stmt.isBlank()) continue;
                    st.execute(stmt.trim());
                    count++;
                }
            }
            log.info("Schema ready ({} statements)", count);
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Could not create tables: " + e.getMessage(), e);
        }
    }
}
