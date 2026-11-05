package ec.edu.espe.mallalab.catalog;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "course", uniqueConstraints = @UniqueConstraint(name = "uq_course_code", columnNames = {"career_id", "code"}))
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "career_id", nullable = false)
    private Career career;

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false)
    private int credits;

    @Column(name = "suggested_term", nullable = false)
    private int suggestedTerm;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prerequisite> prerequisites = new ArrayList<>();

    protected Course() {
    }

    public Course(String code, String name, int credits, int suggestedTerm) {
        this.code = code;
        this.name = name;
        this.credits = credits;
        this.suggestedTerm = suggestedTerm;
    }

    public void addPrerequisite(Course required) {
        Prerequisite prerequisite = new Prerequisite(this, required);
        prerequisites.add(prerequisite);
    }

    public Long getId() {
        return id;
    }

    public void setCareer(Career career) {
        this.career = career;
    }

    public Career getCareer() {
        return career;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getCredits() {
        return credits;
    }

    public int getSuggestedTerm() {
        return suggestedTerm;
    }

    public List<Prerequisite> getPrerequisites() {
        return prerequisites;
    }
}
