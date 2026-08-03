package com.omnixys.address;

import io.github.cdimascio.dotenv.Dotenv;

public class Env {
    static {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(".")
                    .ignoreIfMissing()
                    .load();
            dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        } catch (Exception e) {
            System.err.println("WARN: .env file not found or invalid: " + e.getMessage());
        }
    }
}