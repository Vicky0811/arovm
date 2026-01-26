package com.arovm.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.*;
import java.nio.file.*;

public class TerminalHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 1. Parse the JSON from the IDE
        JsonNode json = mapper.readTree(message.getPayload());
        String code = json.get("code").asText();
        String lang = json.get("language").asText();

        String sessionId = "arovm_" + session.getId();

        new Thread(() -> {
            try {
                Path tempDir = Files.createTempDirectory("arovm_ws_");
                String[] command;

                // 2. Select Docker Image based on Language
                if ("python".equals(lang)) {
                    Files.writeString(tempDir.resolve("script.py"), code);
                    command = new String[]{"docker", "run", "--rm", "--name", sessionId,"--memory", "256m", "--cpus", "0.5", "-v", tempDir.toAbsolutePath() + ":/app", "python:3.11-slim", "python3", "-u", "/app/script.py"};
                } else if ("c_cpp".equals(lang)) {
                    Files.writeString(tempDir.resolve("main.cpp"), code);
                    command = new String[]{"docker", "run", "--rm", "--name", sessionId,"--memory", "256m", "--cpus", "0.5", "-v", tempDir.toAbsolutePath() + ":/app", "gcc:latest", "sh", "-c", "g++ /app/main.cpp -o /app/main && stdbuf -oL /app/main"};
                } else {
                    Files.writeString(tempDir.resolve("Main.java"), code);
                    command = new String[]{"docker", "run", "--rm", "--name", sessionId,"--memory", "256m", "--cpus", "0.5", "-v", tempDir.toAbsolutePath() + ":/app", "eclipse-temurin:21-jdk-jammy", "sh", "-c", "javac /app/Main.java && java -cp /app Main"};
                }

                Process process = new ProcessBuilder(command).start();

                // 3. Read stream line-by-line and PUSH to frontend
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(line + "\n"));
                        }
                    }
                }

                // Capture Errors
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String errLine;
                    while ((errLine = errorReader.readLine()) != null) {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage("Error: " + errLine + "\n"));
                        }
                    }
                }

                process.waitFor();
                if (session.isOpen()) session.sendMessage(new TextMessage("\n[Process Completed]"));

            } catch (Exception e) {
                try { session.sendMessage(new TextMessage("System Error: " + e.getMessage())); } catch (IOException ignored) {}
            }
        }).start();
    }
}