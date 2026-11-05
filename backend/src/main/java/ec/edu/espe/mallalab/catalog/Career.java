package ec.edu.espe.mallalab.catalog;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "career")
public class Career {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 160)
    private String institution;

    @Column(nullable = false)
    private int totalCredits;

    @OneToMany(mappedBy = "career", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Course> courses = new ArrayList<>();

    protected Career() {
    }

    public Career(String code, String name, String institution, int totalCredits) {
        this.code = code;
        this.name = name;
        this.institution = institution;
        this.totalCredits = totalCredits;
    }

    public void addCourse(Course course) {
        course.setCareer(this);
        courses.add(course);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getInstitution() {
        return institution;
    }

    public int getTotalCredits() {
        return totalCredits;
    }

    public List<Course> getCourses() {
        return courses;
    }
}
