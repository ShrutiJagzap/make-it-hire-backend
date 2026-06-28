package com.example.makeItHired.controller;

import com.example.makeItHired.entity.Job;
import com.example.makeItHired.repository.JobRepository;

import com.example.makeItHired.entity.Role;
import com.example.makeItHired.service.NotificationService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobController {
    private final JobRepository jobRepository;
    private final NotificationService notificationService;

    public JobController(JobRepository jobRepository, NotificationService notificationService) {
        this.jobRepository = jobRepository;
        this.notificationService = notificationService;
    }

    // HR creates job
    @PostMapping("/create")
    public Job createJob(@RequestBody Job job) {
        Job savedJob = jobRepository.save(job);
        try {
            notificationService.createNotification(
                null,
                Role.USER,
                "New Job Alert!",
                "New position open: " + savedJob.getJobTitle() + (savedJob.getCompanyName() != null ? " at " + savedJob.getCompanyName() : ""),
                "APPLICATION",
                "/jobs"
            );
        } catch (Exception notifEx) {
            System.err.println("Failed to trigger job creation notification: " + notifEx.getMessage());
        }
        return savedJob;
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
