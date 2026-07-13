package com.JobMatcher.controller;

import com.JobMatcher.entity.Recruiter;
import com.JobMatcher.repository.RecruiterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Optional;

@Controller
@RequestMapping("/recruiter")
public class RecruiterController {

    private final RecruiterRepository recruiterRepository;

    // HARDCODED USER (Simulating a logged-in Recruiter)
    private final String MOCK_USER = "recruiter_01";

    @Autowired
    public RecruiterController(RecruiterRepository recruiterRepository) {
        this.recruiterRepository = recruiterRepository;
    }

    // 1. Single Page: Shows Dashboard with Edit Form AND Preview
    @GetMapping("/profile")
    public String viewProfile(Model model) {
        Optional<Recruiter> opt = recruiterRepository.findByUsername(MOCK_USER);
        Recruiter recruiter;

        if (opt.isPresent()) {
            recruiter = opt.get();
        } else {
            // Create default user if not exists
            recruiter = new Recruiter(MOCK_USER, "Alice", "Recruiter", "alice@company.com");
            recruiterRepository.save(recruiter);
        }

        model.addAttribute("recruiter", recruiter);
        return "recruiter_profile"; // This is your new dashboard HTML
    }

    // 2. Process the Update (Form submits here)
    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute("recruiter") Recruiter formData) {
        Recruiter existing = recruiterRepository.findByUsername(MOCK_USER)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));

        // Update fields
        existing.setFirstName(formData.getFirstName());
        existing.setLastName(formData.getLastName());
        existing.setEmail(formData.getEmail());
        existing.setCompanyName(formData.getCompanyName());
        existing.setCompanyWebsite(formData.getCompanyWebsite());
        existing.setIndustry(formData.getIndustry());
        existing.setLocation(formData.getLocation());
        existing.setBio(formData.getBio());

        recruiterRepository.save(existing);

        // Redirect back to the same page to show updated data
        return "redirect:/recruiter/profile";
    }
}