package com.JobMatcher.entity;

import org.springframework.data.neo4j.core.schema.*;
import lombok.Data;

@Node("Skill")
@Data
public class Skill {
    @Id @GeneratedValue private Long id;
    private String name;
}