package ru.eaze.settings;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import ru.eaze.settings.Settings;
import ru.eaze.settings.SettingsForm;

import javax.swing.*;

/**
 * Created with IntelliJ IDEA.
 * User: gb
 * Date: 23.09.12
 * Time: 21:23
 * To change this template use File | Settings | File Templates.
 */
public class EazeSettingsPanel  implements Configurable {

    private SettingsForm form;
    private Settings settings;

    @Nls
    @Override
    public String getDisplayName() {
        return "EazeStorm";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    @Override
    public JComponent createComponent() {
        if (form == null) {
            DataContext dataContext = DataManager.getInstance().getDataContext();
            Project project = PlatformDataKeys.PROJECT.getData(dataContext);
            settings = new Settings(project);
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
          settings.setStringValue(Settings.KEY_WEB_DIR, form.getPathValue());
    }

    @Override
    public void reset() {

    }

    @Override
    public void disposeUIResources() {

    }
}
