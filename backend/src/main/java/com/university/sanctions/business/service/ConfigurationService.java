package com.university.sanctions.business.service;

import com.university.sanctions.domain.model.SystemConfiguration;
import com.university.sanctions.domain.repository.SystemConfigurationRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationService {
    private final SystemConfigurationRepository repository;

    public ConfigurationService(SystemConfigurationRepository repository) {
        this.repository = repository;
    }

    public int requiredInt(String key) {
        return repository.findById(key).map(row -> {
            try { return Integer.parseInt(row.getValue()); }
            catch (NumberFormatException ex) { throw DomainException.badRequest("INVALID_CONFIGURATION", "Parámetro inválido: " + key); }
        }).orElseThrow(() -> DomainException.badRequest("MISSING_CONFIGURATION", "Falta el parámetro " + key + "."));
    }

    public BigDecimal requiredDecimal(String key) {
        return repository.findById(key).map(row -> {
            try { return new BigDecimal(row.getValue()); }
            catch (NumberFormatException ex) { throw DomainException.badRequest("INVALID_CONFIGURATION", "Parámetro inválido: " + key); }
        }).orElseThrow(() -> DomainException.badRequest("MISSING_CONFIGURATION", "Falta el parámetro " + key + "."));
    }

    public Map<String, Object> all() {
        Map<String, Object> result = new LinkedHashMap<>();
        repository.findAll().stream().sorted((a, b) -> a.getConfigKey().compareTo(b.getConfigKey()))
            .forEach(row -> result.put(row.getConfigKey(), Map.of("value", row.getValue(), "description", row.getDescription())));
        return result;
    }

    public SystemConfiguration set(String key, String value, String description) {
        SystemConfiguration row = repository.findById(key).orElseGet(() -> new SystemConfiguration(key, value, description));
        row.setValue(value);
        row.setDescription(description);
        return repository.save(row);
    }
}