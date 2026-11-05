package ec.edu.espe.mallalab.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Arista del grafo: {@code course} solo puede matricularse si {@code requiredCourse} esta aprobada.
 */
@Entity
@Table(name = "prerequisite", uniqueConstraints = @UniqueConstraint(name = "uq_prerequisite", columnNames = {"course_id", "required_course_id"}))
public class Prerequisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "required_course_id", nullable = false)
    private Course requiredCourse;

    protected Prerequisite() {
    }

    public Prerequisite(Course course, Course requiredCourse) {
        this.course = course;
        this.requiredCourse = requiredCourse;
    }

    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public Course getRequiredCourse() {
        return requiredCourse;
    }
}
