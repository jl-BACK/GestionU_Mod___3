package com.university.sanctions.business.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EvidenceStorage {
    private final Path root;
    public EvidenceStorage(@Value("${app.upload-dir:./storage}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > 5L * 1024 * 1024) throw DomainException.badRequest("EVIDENCE_TOO_LARGE", "La evidencia no puede superar 5 MB.");
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = original.lastIndexOf('.') >= 0 ? original.substring(original.lastIndexOf('.')).replaceAll("[^A-Za-z0-9.]", "") : "";
        try {
            Files.createDirectories(root);
            Path target = root.resolve(UUID.randomUUID() + extension).normalize();
            if (!target.startsWith(root)) throw DomainException.badRequest("INVALID_EVIDENCE", "Archivo de evidencia inválido.");
            file.transferTo(target);
            return target.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo almacenar la evidencia.", ex);
        }
    }
}