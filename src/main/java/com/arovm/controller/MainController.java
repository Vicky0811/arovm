package com.arovm.controller;

import java.io.*;
import java.nio.file.*;
import java.util.*;

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

    @GetMapping("/")
    public String home(Model model) {
        // Get the current logged-in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Check if they are logged in and not "anonymous"
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            // Get the username directly from the database login
            String username = auth.getName();
            model.addAttribute("username", username);
        }

        return "index";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user) {
        // Check if user already exists
        if (userRepository.findByUsername(user.getUsername()) != null) {
            return "redirect:/?error=UserAlreadyExists";
        }

        // Save user (Plain text password for now, as per your setup)
        userRepository.save(user);

        // Redirect to home and trigger the login modal (optional, or just let them click login)
        return "redirect:/";
    }
    @GetMapping("/ide")
    public String showIdePage() {
        return "ide"; // Requires an ide.html template
    }
    @GetMapping("/founder")
    public String showFounderProfile() {
        return "founder";
    }
    @Autowired
    private ProjectRepository projectRepository;

    @PostMapping("/api/projects/save")
    public ResponseEntity<String> saveProject(@RequestBody Project project) {
        try {
            // If a project with this name exists, it will update; otherwise, it creates new.
            projectRepository.save(project);
            return ResponseEntity.ok("Project '" + project.getName() + "' saved successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error saving project: " + e.getMessage());
        }
    }
    // Use a ConcurrentHashMap to track running processes if you want to be precise
    private final Map<String, Process> activeProcesses = new java.util.concurrent.ConcurrentHashMap<>();

    @PostMapping("/api/run")
    public String runInDocker(@RequestBody CodeRequest request) {
        String sessionId = "arovm_" + Thread.currentThread().getId();
        try {
            Path tempDir = Files.createTempDirectory("arovm_run_");
            File sourceFile = new File(tempDir.toFile(), "Main.java");
            Files.writeString(sourceFile.toPath(), request.getCode());

            // Added --name to the docker command
            String[] command = {
                    "docker", "run", "--rm",
                    "--name", sessionId,
                    "-v", tempDir.toAbsolutePath() + ":/app",
                    "openjdk:17-slim",
                    "sh", "-c", "javac /app/Main.java && java -cp /app Main"
            };

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            activeProcesses.put(sessionId, process); // Track it

            String output = readStream(process.getInputStream());
            activeProcesses.remove(sessionId); // Remove when done

            return output;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    // Helper method to read the process streams
    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    // Helper method to clean up temp files
    private void deleteDirectory(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) deleteDirectory(f);
        }
        file.delete();
    }
    @GetMapping("/api/projects/all")
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }
    @PostMapping("/api/stop")
    public ResponseEntity<String> stopCode() {
        try {
            // This command kills all containers started by the Arovm IDE
            // It's a "Global Stop" for simplicity on your laptop
            String[] stopCommand = {"sh", "-c", "docker ps -q --filter \"name=arovm_\" | xargs -r docker stop"};
            Runtime.getRuntime().exec(stopCommand);

            return ResponseEntity.ok("Execution Stopped.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Stop Failed: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/projects/delete/{id}")
    public ResponseEntity<String> deleteProject(@PathVariable Long id) {
        try {
            projectRepository.deleteById(id);
            return ResponseEntity.ok("Project deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting project: " + e.getMessage());
        }
    }


}