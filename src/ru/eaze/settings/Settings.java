package ru.eaze.settings;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 * User: GGobozov
 * Date: 28.11.12
 * Time: 14:33
 * To change this template use File | Settings | File Templates.
 */
public class Settings {

    public static final String KEY_WEB_DIR = "webdir";
    @NotNull
    private Project project;

    public Settings(Project project) {
        this.project = project;
    }


    public String getStringValue(String key, String defaultValue) {
        return PropertiesComponent.getInstance(project).getValue(key,defaultValue);
    }

    public void setStringValue (String key, String value){
        PropertiesComponent.getInstance(project).setValue(key, value);
    }


}
