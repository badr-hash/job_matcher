package com.JobMatcher.repository;

import com.JobMatcher.entity.Candidate;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface CandidateRepository extends Neo4jRepository<Candidate, Long> {
    Candidate findByEmail(String email);
}