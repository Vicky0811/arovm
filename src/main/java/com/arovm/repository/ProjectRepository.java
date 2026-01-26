package com.arovm.repository;

import com.arovm.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    // No code needed here! Spring handles the implementation.
}