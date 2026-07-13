package com.JobMatcher.repository;

import com.JobMatcher.entity.Recruiter;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import java.util.Optional;

public interface RecruiterRepository extends Neo4jRepository<Recruiter, Long> {
    Optional<Recruiter> findByUsername(String username);
}