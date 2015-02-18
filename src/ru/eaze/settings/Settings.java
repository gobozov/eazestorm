package ru.eaze.settings;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class Settings {

    public static final String KEY_WEB_DIR = "eazestorm.webdir";

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
        return PropertiesComponent.getInstance(project).getValue(KEY_WEB_DIR, "web/");
    }

    public void setWebDir(@NotNull String webDir) {
        PropertiesComponent.getInstance(project).setValue(KEY_WEB_DIR, webDir);
    }
}
