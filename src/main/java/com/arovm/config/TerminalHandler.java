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
    private static final long MAX_EXECUTION_TIME_SECONDS = 30;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode json = mapper.readTree(message.getPayload());
        String code = json.get("code").asText();
        String lang = json.get("language").asText();
        // Sanitize Session ID for Docker Name
        String sessionId = "arovm_" + session.getId().replaceAll("[^a-zA-Z0-9]", "");

        new Thread(() -> {
            Process process = null;
            Path tempFile = null;
            try {
                // 1. Create Code File Locally (Inside Java Container)
                Path tempDir = Files.createTempDirectory("arovm_ws_");
                String fileName = "python".equals(lang) ? "script.py" : (lang.contains("c") ? "main.cpp" : "Main.java");
                tempFile = tempDir.resolve(fileName);
                Files.writeString(tempFile, code);

                // 2. PREPARE the Container (Create but don't start)
                // We define the command here but don't run it yet.
                ProcessBuilder createPb = getCreateCommand(lang, sessionId);
                if (createPb.start().waitFor() != 0) {
                    send(session, "Error: Failed to initialize container.");
                    return;
                }

                // 3. COPY the file from Java Container -> Runner Container
                // "docker cp <local_path> <container_name>:<dest_path>"
                ProcessBuilder copyPb = new ProcessBuilder(
                        "docker", "cp", tempFile.toAbsolutePath().toString(), sessionId + ":/app/" + fileName
                );
                if (copyPb.start().waitFor() != 0) {
                    send(session, "Error: Failed to upload code to sandbox.");
                    stopDockerContainer(sessionId);
                    return;
                }

                // 4. START the Container (Attach to Output)
                // "docker start -a <container_name>"
                process = new ProcessBuilder("docker", "start", "-a", sessionId).start();

                // 5. Stream Output
                startStreaming(session, process.getInputStream(), "");
                startStreaming(session, process.getErrorStream(), "Error: ");

                // 6. Time Limit Enforcement
                boolean finished = process.waitFor(MAX_EXECUTION_TIME_SECONDS, TimeUnit.SECONDS);

                if (!finished) {
                    send(session, "\n[Time Limit Exceeded: Process Terminated]");
                    stopDockerContainer(sessionId);
                } else {
                    send(session, "\n[Process Completed]");
                }

            } catch (Exception e) {
                send(session, "System Error: " + e.getMessage());
            } finally {
                if (process != null) process.destroyForcibly();
                stopDockerContainer(sessionId); // Always clean up
                if (tempFile != null) {
                    try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
                }
            }
        }).start();
    }

    // Helper to generate the specific Docker Create command
    private ProcessBuilder getCreateCommand(String lang, String sessionId) {
        // Base constraints: 128MB RAM, 0.5 CPU, No Network, Auto-remove disabled (we remove manually)
        String base = "docker create --name " + sessionId + " --memory 128m --cpus 0.5 --network none -w /app ";

        if ("python".equals(lang)) {
            // Python Command
            return new ProcessBuilder((base + "python:3.11-slim python3 -u script.py").split(" "));
        } else if ("c_cpp".equals(lang)) {
            // C++ Command: Compile then Run
            return new ProcessBuilder((base + "gcc:latest sh -c").split(" ")).command(
                    "docker", "create", "--name", sessionId, "--memory", "128m", "--cpus", "0.5",
                    "--network", "none", "-w", "/app", "gcc:latest",
                    "sh", "-c", "g++ -o main main.cpp && ./main"
            );
        } else {
            // Java Command
            return new ProcessBuilder((base + "eclipse-temurin:21-jdk-jammy sh -c").split(" ")).command(
                    "docker", "create", "--name", sessionId, "--memory", "128m", "--cpus", "0.5",
                    "--network", "none", "-w", "/app", "eclipse-temurin:21-jdk-jammy",
                    "sh", "-c", "javac Main.java && java Main"
            );
        }
    }

    private void startStreaming(WebSocketSession session, InputStream stream, String prefix) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null && session.isOpen()) {
                    synchronized (session) { // Synchronize to prevent message mixing
                        session.sendMessage(new TextMessage(prefix + line + "\n"));
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void stopDockerContainer(String containerName) {
        try { new ProcessBuilder("docker", "rm", "-f", containerName).start().waitFor(); } catch (Exception ignored) {}
    }

    private void send(WebSocketSession session, String msg) {
        try { if (session.isOpen()) session.sendMessage(new TextMessage(msg)); } catch (IOException ignored) {}
    }
}
