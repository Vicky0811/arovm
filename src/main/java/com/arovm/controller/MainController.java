package com.arovm.controller;

import com.arovm.model.User;
import com.arovm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/founder")
    public String showFounderProfile() {
        return "founder";
    }
}