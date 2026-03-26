package com.example.makeItHired.controller;

import com.example.makeItHired.entity.Job;
import com.example.makeItHired.repository.JobRepository;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobController {
    private final JobRepository jobRepository;

    public JobController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    // HR creates job
    @PostMapping("/create")
    public Job createJob(@RequestBody Job job) {
        return jobRepository.save(job);
    }

    // fetch all jobs
    @GetMapping
    public List<Job> getJobs() {
        return jobRepository.findAll();
    }

    // job details
    @GetMapping("/{id}")
    public Job getJob(@PathVariable Long id) {
        return jobRepository.findById(id).orElseThrow();
    }
}
