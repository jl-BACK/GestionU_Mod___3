package com.university.sanctions.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_ref")
@Getter @Setter @NoArgsConstructor
public class StudentRef {
    @Id
    @Column(name = "student_code", length = 40)
    private String studentCode;
    @Column(nullable = false, length = 180)
    private String name;

    public StudentRef(String studentCode, String name) {
        this.studentCode = studentCode;
        this.name = name;
    }
}