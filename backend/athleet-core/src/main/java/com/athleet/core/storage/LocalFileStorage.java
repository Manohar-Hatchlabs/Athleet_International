package com.athleet.core.storage;

import com.athleet.core.common.error.NotFoundException;
import com.athleet.core.storage.config.StorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

/** Development storage on the local disk. The root must be outside any web root and outside the repository. */
@Component
@ConditionalOnProperty(name = "athleet.storage.provider", havingValue = "local")
public class LocalFileStorage implements FileStorage {

    private static final Pattern SAFE_KEY = Pattern.compile("^[a-z0-9][a-z0-9/._-]{0,199}$");

    private final Path root;

    public LocalFileStorage(StorageProperties props) {
        String configured = props.localRoot();
        Path base = configured == null || configured.isBlank()
                ? Path.of(System.getProperty("user.home"), ".athleet", "files")
                : Path.of(configured);
        this.root = base.toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create the storage folder", e);
        }
    }

    @Override
    public void put(String key, byte[] bytes) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Path tmp = Files.createTempFile(target.getParent(), "upload-", ".tmp");
            try {
                Files.write(tmp, bytes);
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store the file", e);
        }
    }

    @Override
    public InputStream open(String key) {
        Path target = resolve(key);
        try {
            return Files.newInputStream(target);
        } catch (java.nio.file.NoSuchFileException e) {
            throw new NotFoundException("File not found.");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the file", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete the file", e);
        }
    }

    private Path resolve(String key) {
        if (key == null || !SAFE_KEY.matcher(key).matches() || key.contains("..")) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return target;
    }
}
