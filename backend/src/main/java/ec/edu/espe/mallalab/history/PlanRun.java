package ec.edu.espe.mallalab.history;

import ec.edu.espe.mallalab.catalog.Career;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Historial de simulaciones guardadas por el usuario. */
@Entity
@Table(name = "plan_run")
public class PlanRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "career_id", nullable = false)
    private Career career;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved_count", nullable = false)
    private int approvedCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "failed_codes", length = 4000)
    private String failedCodes;

    @Column(name = "max_credits_per_term", nullable = false)
    private int maxCreditsPerTerm;

    @Column(name = "total_terms", nullable = false)
    private int totalTerms;

    @Column(name = "delay_terms", nullable = false)
    private int delay;

    protected PlanRun() {
    }

    public PlanRun(Career career, int approvedCount, int failedCount, String failedCodes,
                   int maxCreditsPerTerm, int totalTerms, int delay) {
        this.career = career;
        this.createdAt = LocalDateTime.now();
        this.approvedCount = approvedCount;
        this.failedCount = failedCount;
        this.failedCodes = failedCodes;
        this.maxCreditsPerTerm = maxCreditsPerTerm;
        this.totalTerms = totalTerms;
        this.delay = delay;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getApprovedCount() {
        return approvedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public String getFailedCodes() {
        return failedCodes;
    }

    public int getMaxCreditsPerTerm() {
        return maxCreditsPerTerm;
    }

    public int getTotalTerms() {
        return totalTerms;
    }

    public int getDelay() {
        return delay;
    }
}
