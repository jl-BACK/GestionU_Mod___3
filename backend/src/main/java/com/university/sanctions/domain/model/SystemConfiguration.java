package com.university.sanctions.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "system_configuration")
@Getter @Setter @NoArgsConstructor
public class SystemConfiguration {
    @Id
    @Column(name = "config_key", length = 100)
    private String configKey;
    @Column(nullable = false, length = 500)
    private String value;
    @Column(nullable = false, length = 500)
    private String description;

    public SystemConfiguration(String configKey, String value, String description) {
        this.configKey = configKey;
        this.value = value;
        this.description = description;
    }
}