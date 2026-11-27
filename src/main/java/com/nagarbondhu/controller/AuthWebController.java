package com.nagarbondhu.controller;

import com.nagarbondhu.dto.request.RegisterRequest;
import com.nagarbondhu.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthWebController {

    private final AuthService authService;

    @GetMapping("/")
    public String index(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("authenticated", true);
            model.addAttribute("username", authentication.getName());
        } else {
            model.addAttribute("authenticated", false);
        }
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        try {
            authService.register(request);
            return "redirect:/login?registered";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }
        model.addAttribute("authenticated", true);
        model.addAttribute("username", authentication.getName());
        return "index";
    }
}
