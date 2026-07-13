package com.JobMatcher.entity;

import org.springframework.data.neo4j.core.schema.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

@Node("Job")
@Data
public class Job {
    @Id @GeneratedValue private Long id;

    private String title;
    private String description;
    private String location;

    private String workType;
    private String schedule;
    private String companyName;
    private String recruiterName;
    private LocalDate postedDate;

    @Relationship(type = "REQUIRES_SKILL", direction = Relationship.Direction.OUTGOING)
    private List<Skill> skills = new ArrayList<>();
}