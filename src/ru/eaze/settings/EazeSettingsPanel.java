package ru.eaze.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import ru.eaze.indexes.IndexUtil;

import javax.swing.*;

public class EazeSettingsPanel implements Configurable {

    private SettingsForm form;
    private Settings settings;

    private final Project project;

    public EazeSettingsPanel(@NotNull Project project) {
        this.project = project;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "EazeStorm";
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public JComponent createComponent() {
        if (form == null) {
            settings = Settings.forProject(project);
            form = new SettingsForm(settings);
        }
        return form;
    }

    @Override
    public boolean isModified() {
        return form.isModified();
    }

    @Override
    public void apply() throws ConfigurationException {
        form.apply(settings);
        IndexUtil.reindex();
    }

    @Override
    public void reset() {
        form.reset(settings);
    }

    @Override
    public void disposeUIResources() {
        form.dispose();
    }
}
