package com.JobMatcher.entity;

import org.springframework.data.neo4j.core.schema.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Node("Candidate")
@Data
public class Candidate {
    @Id @GeneratedValue private Long id;

    private String name;
    private String email;
    private String password;

    private String role;
    private String phone;
    private String city;
    private String companyName;

    private String title;
    private Integer profileViews;
    private Integer postViews;

    @Relationship(type = "HAS_SKILL", direction = Relationship.Direction.OUTGOING)
    private List<Skill> skills = new ArrayList<>();

    @Relationship(type = "APPLIED_TO", direction = Relationship.Direction.OUTGOING)
    private List<Job> appliedJobs = new ArrayList<>();

    @Relationship(type = "SAVED_JOB", direction = Relationship.Direction.OUTGOING)
    private List<Job> savedJobs = new ArrayList<>();

    // STOCKAGE DU CV
    private String cvFileName;
    private String cvData;

    private String profilePicture; // URL ou Base64
    private String coverPicture;   // URL ou Base64
}