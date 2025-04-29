package com.homework.project3.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String showHomePage() {
        return "home";
    }

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        if (model.containsAttribute("logout")) {
            model.addAttribute("message", "You have been successfully logged out.");
        }
        return "login";
    }

    @GetMapping("/logout")
    public String showLogoutPage() {
        return "logout";
    }
}