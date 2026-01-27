package com.arovm.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

public class TerminalHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final long MAX_EXECUTION_TIME_SECONDS = 30; // 30-second limit

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = mapper.readTree(message.getPayload());
        String code = json.get("code").asText();
        String lang = json.get("language").asText();
        String sessionId = "arovm_" + session.getId().replaceAll("[^a-zA-Z0-9]", "");

        new Thread(() -> {
            Process process = null;
            try {
                Path tempDir = Files.createTempDirectory("arovm_ws_");
                String[] command;

                // Added --stop-timeout 1 and resource constraints
                String dockerBase = "docker run --rm --name " + sessionId + " --memory 128m --cpus 0.5 --stop-timeout 1 -v " + tempDir.toAbsolutePath() + ":/app ";

                if ("python".equals(lang)) {
                    Files.writeString(tempDir.resolve("script.py"), code);
                    command = (dockerBase + "python:3.11-slim python3 -u /app/script.py").split(" ");
                } else if ("c_cpp".equals(lang)) {
                    Files.writeString(tempDir.resolve("main.cpp"), code);
                    command = new String[]{"docker", "run", "--rm", "--name", sessionId, "--memory", "128m", "--cpus", "0.5", "-v", tempDir.toAbsolutePath() + ":/app", "gcc:latest", "sh", "-c", "g++ /app/main.cpp -o /app/main && stdbuf -oL /app/main"};
                } else {
                    Files.writeString(tempDir.resolve("Main.java"), code);
                    command = new String[]{"docker", "run", "--rm", "--name", sessionId, "--memory", "128m", "--cpus", "0.5", "-v", tempDir.toAbsolutePath() + ":/app", "eclipse-temurin:21-jdk-jammy", "sh", "-c", "javac /app/Main.java && java -cp /app Main"};
                }

                process = new ProcessBuilder(command).start();

                // Stream Output
                InputStream inputStream = process.getInputStream();
                InputStream errorStream = process.getErrorStream();

                startStreaming(session, inputStream, "");
                startStreaming(session, errorStream, "Error: ");

                // 4. TIME BOUND LOGIC: Wait for X seconds max
                boolean finished = process.waitFor(MAX_EXECUTION_TIME_SECONDS, TimeUnit.SECONDS);

                if (!finished) {
                    if (session.isOpen()) session.sendMessage(new TextMessage("\n[Time Limit Exceeded: Process Terminated]"));
                    stopDockerContainer(sessionId); // Force kill the container
                }

                if (session.isOpen()) session.sendMessage(new TextMessage("\n[Process Completed]"));

            } catch (Exception e) {
                try { if (session.isOpen()) session.sendMessage(new TextMessage("System Error: " + e.getMessage())); } catch (IOException ignored) {}
            } finally {
                if (process != null) process.destroyForcibly();
                stopDockerContainer(sessionId); // Double check cleanup
            }
        }).start();
    }

    private void startStreaming(WebSocketSession session, InputStream stream, String prefix) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null && session.isOpen()) {
                    session.sendMessage(new TextMessage(prefix + line + "\n"));
                }
            } catch (IOException ignored) {}
        }).start();
    }

    private void stopDockerContainer(String containerName) {
        try {
            // Forcefully remove the container if it's still hanging
            new ProcessBuilder("docker", "rm", "-f", containerName).start();
        } catch (IOException e) {
            System.err.println("Failed to cleanup container: " + containerName);
        }
    }
}
