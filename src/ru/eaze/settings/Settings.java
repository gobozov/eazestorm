package ru.eaze.settings;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class Settings {

    private static final String KEY_WEB_DIR = "eazestorm.webdir";
    private static final String KEY_PAGES_CACHE_CHECKSUM_ENABLED = "eazestorm.pages.cache.checksum.enabled";

    private static final String DEFAULT_WEB_DIR = "web/";
    private static final String DEFAULT_PAGES_CACHE_CHECKSUM_ENABLED = Boolean.TRUE.toString();

    @NotNull
    private final Project project;

    private Settings(@NotNull Project project) {
        this.project = project;
    }

    public static Settings forProject(@NotNull Project project) {
        return new Settings(project);
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public String getWebDir() {
        return PropertiesComponent.getInstance(project).getValue(KEY_WEB_DIR, DEFAULT_WEB_DIR);
    }

    public void setWebDir(@NotNull String webDir) {
        PropertiesComponent.getInstance(project).setValue(KEY_WEB_DIR, webDir, DEFAULT_WEB_DIR);
    }

    public boolean isPagesCacheChecksumEnabled() {
        return Boolean.parseBoolean(PropertiesComponent.getInstance(project).getValue(KEY_PAGES_CACHE_CHECKSUM_ENABLED, DEFAULT_PAGES_CACHE_CHECKSUM_ENABLED));
    }

    public void setPagesCacheChecksumEnabled(boolean enabled) {
        PropertiesComponent.getInstance(project).setValue(KEY_PAGES_CACHE_CHECKSUM_ENABLED, Boolean.toString(enabled), DEFAULT_PAGES_CACHE_CHECKSUM_ENABLED);
    }
}
