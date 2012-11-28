package ru.eaze;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;

/**
 * Created with IntelliJ IDEA.
 * User: GGobozov
 * Date: 28.11.12
 * Time: 14:33
 * To change this template use File | Settings | File Templates.
 */
public class Settings {

    public static final String KEY_WEB_DIR = "webdir";
    private static Project project;

    public static void initSettings(Project project) {
        project = project;
    }

    private static String getStringValue(String key, String defaultValue) {
        try {
            if (project == null)
                throw new Exception("Settings are not initialized, call initSettings() before!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return PropertiesComponent.getInstance(project).getValue(@NonNls key, @NonNls defaultValue);
    }

    private static void setStringValue (String key, String value){
        PropertiesComponent.getInstance(project).setValue(key, value);
    }

    public void setWebDir(String webDirName){
        setStringValue(KEY_WEB_DIR, webDirName);
    }

    public String getWebDir(String defaultValue){
        return getStringValue(KEY_WEB_DIR, defaultValue);
    }

    public String getWebDir(){
        return getStringValue(KEY_WEB_DIR, "web");
    }




}
