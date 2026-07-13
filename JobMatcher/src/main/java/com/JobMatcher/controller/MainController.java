package com.JobMatcher.controller;

import com.JobMatcher.entity.*;
import com.JobMatcher.repository.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MainController {

    private final JobRepository jobRepo;
    private final CandidateRepository candRepo;

    public MainController(JobRepository jobRepo, CandidateRepository candRepo) {
        this.jobRepo = jobRepo;
        this.candRepo = candRepo;
    }

    // ==========================================
    // 1. AUTH & INITIALISATION
    // ==========================================

    @PostMapping("/signup")
    public Candidate signup(@RequestBody Candidate c) {
        if ("candidate".equals(c.getRole())) {
            if (c.getTitle() == null) c.setTitle("Nouveau Membre");
            c.setProfileViews(0);
            c.setPostViews(0);
        }
        return candRepo.save(c);
    }

    @PostMapping("/login")
    public Candidate login(@RequestBody Map<String, String> data) {
        Candidate c = candRepo.findByEmail(data.get("email"));
        if (c != null && c.getPassword().equals(data.get("password"))) {
            return c;
        }
        throw new RuntimeException("Login failed");
    }

    // ==========================================
    // 2. GESTION CANDIDAT (Update & Images) - NOUVEAU
    // ==========================================

    // Mettre à jour les infos textes (Nom, Titre, Ville, Tel...)
    @PutMapping("/candidates/{id}")
    public Candidate updateCandidate(@PathVariable Long id, @RequestBody Candidate updates) {
        return candRepo.findById(id).map(c -> {
            if(updates.getName() != null) c.setName(updates.getName());
            if(updates.getTitle() != null) c.setTitle(updates.getTitle());
            if(updates.getCity() != null) c.setCity(updates.getCity());
            if(updates.getPhone() != null) c.setPhone(updates.getPhone());
            if(updates.getProfilePicture() != null) c.setProfilePicture(updates.getProfilePicture()); // Pour URL web
            if(updates.getCoverPicture() != null) c.setCoverPicture(updates.getCoverPicture());     // Pour URL web
            return candRepo.save(c);
        }).orElseThrow(() -> new RuntimeException("Candidat introuvable"));
    }

    // Upload Photo de Profil (Fichier local)
    @PostMapping("/candidates/{id}/upload-photo")
    public Candidate uploadProfilePic(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        Candidate c = candRepo.findById(id).orElseThrow();
        // On stocke l'image en Base64 avec le préfixe data:image
        String base64Image = "data:" + file.getContentType() + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
        c.setProfilePicture(base64Image);
        return candRepo.save(c);
    }

    // Upload Photo de Couverture (Fichier local)
    @PostMapping("/candidates/{id}/upload-cover")
    public Candidate uploadCoverPic(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        Candidate c = candRepo.findById(id).orElseThrow();
        String base64Image = "data:" + file.getContentType() + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
        c.setCoverPicture(base64Image);
        return candRepo.save(c);
    }

    // ==========================================
    // 3. GESTION JOBS
    // ==========================================

    @PostMapping("/jobs")
    public Job createJob(@RequestBody Job j, @RequestParam Long recruiterId) {
        Candidate recruiter = candRepo.findById(recruiterId)
                .orElseThrow(() -> new RuntimeException("Recruteur introuvable"));
        j.setCompanyName(recruiter.getCompanyName());
        j.setRecruiterName(recruiter.getName());
        j.setPostedDate(LocalDate.now());
        return jobRepo.save(j);
    }

    @GetMapping("/jobs")
    public List<Job> getAllJobs() {
        return jobRepo.findAll();
    }

    @GetMapping("/jobs/{jobId}/applicants")
    public List<Candidate> getApplicants(@PathVariable Long jobId) {
        return candRepo.findAll().stream()
                .filter(c -> c.getAppliedJobs().stream().anyMatch(j -> j.getId().equals(jobId)))
                .collect(Collectors.toList());
    }

    // ==========================================
    // 4. GESTION CV
    // ==========================================

    @PostMapping("/candidates/{id}/upload-cv")
    public Candidate uploadCv(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        Candidate c = candRepo.findById(id).orElseThrow(() -> new RuntimeException("Candidat introuvable"));

        String base64Content = Base64.getEncoder().encodeToString(file.getBytes());
        c.setCvData(base64Content);
        c.setCvFileName(file.getOriginalFilename());

        String cvText = "";
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            cvText = stripper.getText(document).toLowerCase();
        } catch (Exception e) {
            System.err.println("Erreur lecture PDF : " + e.getMessage());
        }

        List<String> knownSkills = Arrays.asList(
                "java", "python", "javascript", "typescript", "c#", "c++", "php", "ruby", "go",
                "spring", "spring boot", "react", "angular", "vue", "node.js", "django", "flask",
                "sql", "mysql", "postgresql", "mongodb", "oracle", "redis",
                "docker", "kubernetes", "aws", "azure", "git", "linux", "jenkins", "devops"
        );

        for (String tech : knownSkills) {
            if (cvText.contains(tech)) {
                String skillName = tech.substring(0, 1).toUpperCase() + tech.substring(1);
                boolean exists = c.getSkills().stream().anyMatch(s -> s.getName().equalsIgnoreCase(skillName));
                if (!exists) {
                    Skill s = new Skill(); s.setName(skillName); c.getSkills().add(s);
                }
            }
        }
        return candRepo.save(c);
    }

    @GetMapping("/candidates/{id}/cv")
    public ResponseEntity<byte[]> downloadCv(@PathVariable Long id) {
        Candidate c = candRepo.findById(id).orElseThrow();
        if (c.getCvData() == null) throw new RuntimeException("Pas de CV");
        byte[] fileBytes = Base64.getDecoder().decode(c.getCvData());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + c.getCvFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(fileBytes);
    }

    // ==========================================
    // 5. SKILLS & MATCHING
    // ==========================================

    @PostMapping("/candidates/{id}/skills")
    public Candidate addSkill(@PathVariable Long id, @RequestBody Skill skill) {
        return candRepo.findById(id).map(c -> {
            boolean exists = c.getSkills().stream().anyMatch(s -> s.getName().equalsIgnoreCase(skill.getName()));
            if (!exists) { c.getSkills().add(skill); return candRepo.save(c); }
            return c;
        }).orElseThrow();
    }

    @DeleteMapping("/candidates/{id}/skills")
    public Candidate removeSkill(@PathVariable Long id, @RequestBody Skill skill) {
        return candRepo.findById(id).map(c -> {
            c.getSkills().removeIf(s -> s.getName().equalsIgnoreCase(skill.getName()));
            return candRepo.save(c);
        }).orElseThrow();
    }

    @PostMapping("/candidates/{id}/save/{jobId}")
    public Candidate toggleSaveJob(@PathVariable Long id, @PathVariable Long jobId) {
        Candidate c = candRepo.findById(id).orElseThrow();
        Job j = jobRepo.findById(jobId).orElseThrow();
        boolean alreadySaved = c.getSavedJobs().stream().anyMatch(job -> job.getId().equals(jobId));
        if (alreadySaved) c.getSavedJobs().removeIf(job -> job.getId().equals(jobId));
        else c.getSavedJobs().add(j);
        return candRepo.save(c);
    }

    @PostMapping("/apply/{candidateId}/{jobId}")
    public Candidate apply(@PathVariable Long candidateId, @PathVariable Long jobId) {
        Candidate c = candRepo.findById(candidateId).orElseThrow();
        Job j = jobRepo.findById(jobId).orElseThrow();
        boolean alreadyApplied = c.getAppliedJobs().stream().anyMatch(job -> job.getId().equals(jobId));
        if (!alreadyApplied) { c.getAppliedJobs().add(j); return candRepo.save(c); }
        return c;
    }

    @GetMapping("/recommendations/{id}")
    public List<Job> getMatches(@PathVariable Long id) {
        Candidate c = candRepo.findById(id).orElseThrow();
        List<String> mySkills = c.getSkills().stream().map(s -> s.getName().toLowerCase()).collect(Collectors.toList());
        String myCity = (c.getCity() == null) ? "" : c.getCity().toLowerCase();
        List<Job> allJobs = jobRepo.findAll();
        allJobs.sort((j1, j2) -> {
            long score1 = countMatches(j1, mySkills);
            long score2 = countMatches(j2, mySkills);
            if (j1.getLocation() != null && j1.getLocation().equalsIgnoreCase(myCity)) score1 += 2;
            if (j2.getLocation() != null && j2.getLocation().equalsIgnoreCase(myCity)) score2 += 2;
            return Long.compare(score2, score1);
        });
        return allJobs;
    }

    private long countMatches(Job j, List<String> candidateSkills) {
        if (j.getSkills() == null) return 0;
        return j.getSkills().stream().filter(s -> candidateSkills.contains(s.getName().toLowerCase())).count();
    }
}