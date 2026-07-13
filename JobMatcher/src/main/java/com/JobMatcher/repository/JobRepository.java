package com.JobMatcher.repository;

import com.JobMatcher.entity.Job;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface JobRepository extends Neo4jRepository<Job, Long> {
}