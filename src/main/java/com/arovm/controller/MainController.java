package com.arovm.controller;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.arovm.model.Project;
import com.arovm.model.User;
import com.arovm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class MainController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    // --- VIEW ROUTES (Returning HTML) ---

    @GetMapping("/")
    public String home(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            model.addAttribute("username", auth.getName());
        }
         return "index";
    }

    @GetMapping("/ide")
    public String showIdePage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            // Passing the actual logged-in user details to ide.html
            model.addAttribute("username", auth.getName());
            model.addAttribute("profileId", auth.getName()); // Using username as profileId
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("profileId", "guest_arovm");
        }
        return "ide";
    }

    @GetMapping("/founder")
    public String showFounderProfile() {
        return "founder";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user) {
        if (userRepository.findByUsername(user.getUsername()) != null) {
            return "redirect:/?error=UserAlreadyExists";
        }
        userRepository.save(user);
        return "redirect:/";
    }

    // --- API ROUTES (Returning JSON/Data) ---

    @PostMapping("/api/projects/save")
    @ResponseBody
    public String save(@RequestBody Project p) {
        projectRepository.save(p);
        return "Saved successfully!";
    }

    @GetMapping("/api/projects/my-projects/{profileId}")
    @ResponseBody
    public List<Project> getMyProjects(@PathVariable String profileId) {
        return projectRepository.findByProfileId(profileId);
    }

    @GetMapping("/api/projects/all")
    @ResponseBody
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    private final Map<String, Process> activeProcesses = new ConcurrentHashMap<>();

    @PostMapping("/api/run")
    @ResponseBody
    public String runInDocker(@RequestBody CodeRequest request) {
        String sessionId = "arovm_" + System.currentTimeMillis();
        String language = request.getLanguage().toLowerCase();
        String code = request.getCode();

        try {
            Path tempDir = Files.createTempDirectory("arovm_run_");
            String[] command = new String[0];

            if ("java".equals(language)) {
                File sourceFile = new File(tempDir.toFile(), "Main.java");
                Files.writeString(sourceFile.toPath(), code);

                command = new String[]{
                        "docker", "run", "--rm",
                        "--name", sessionId,
                        "-v", tempDir.toAbsolutePath() + ":/app",
                        "eclipse-temurin:21-jdk-jammy",
                        "sh", "-c", "javac /app/Main.java && java -cp /app Main"
                };
            } else if ("python".equals(language)) {
                // Python Execution Logic
                File sourceFile = new File(tempDir.toFile(), "script.py");
                Files.writeString(sourceFile.toPath(), code);

                command = new String[]{
                        "docker", "run", "--rm", "--name", sessionId,
                        "-v", tempDir.toAbsolutePath() + ":/app",
                        "python:3.11-slim",
                        "python3", "/app/script.py"
                };
            }else if ("c_cpp".equals(language)) {
                // C++ Execution Logic
                File sourceFile = new File(tempDir.toFile(), "main.cpp");
                Files.writeString(sourceFile.toPath(), code);

                command = new String[]{
                        "docker", "run", "--rm", "--name", sessionId,
                        "-v", tempDir.toAbsolutePath() + ":/app",
                        "gcc:latest",
                        "sh", "-c", "g++ /app/main.cpp -o /app/main && /app/main"
                };
            }else{
                return "--Select a language--";
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            activeProcesses.put(sessionId, process);

            String output = readStream(process.getInputStream());
            String error = readStream(process.getErrorStream());

            activeProcesses.remove(sessionId);
            deleteDirectory(tempDir.toFile());

            return error.isEmpty() ? output : "Execution Error:\n" + error;
        } catch (Exception e) {
            return "System Error: " + e.getMessage();
        }
    }

    @PostMapping("/api/stop")
    @ResponseBody
    public ResponseEntity<String> stopCode() {
        try {
            String[] stopCommand = {"sh", "-c", "docker ps -q --filter \"name=arovm_\" | xargs -r docker stop"};
            Runtime.getRuntime().exec(stopCommand);
            return ResponseEntity.ok("Execution Stopped.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Stop Failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/projects/delete/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteProject(@PathVariable Long id) {
        try {
            projectRepository.deleteById(id);
            return ResponseEntity.ok("Project deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting project: " + e.getMessage());
        }
    }

    // --- HELPERS ---

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private void deleteDirectory(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) deleteDirectory(f);
        }
        file.delete();
    }

    // Nested class for Request mapping
    public static class CodeRequest {
        private String code;
        private String language;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }
}