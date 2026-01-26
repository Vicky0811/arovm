package com.arovm.repository;

import com.arovm.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    // No code needed here! Spring handles the implementation.
    // Spring Data JPA automatically creates the SQL:
    // SELECT * FROM projects WHERE profile_id = ?
    List<Project> findByProfileId(String profileId);
}
