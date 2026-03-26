package com.example.makeItHired.repository;

import com.example.makeItHired.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long>{
}
