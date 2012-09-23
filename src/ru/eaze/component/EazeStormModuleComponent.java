package ru.eaze.component;

import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA.
 * User: user
 * Date: 26.01.12
 * Time: 23:44
 * To change this template use File | Settings | File Templates.
 */
public class EazeStormModuleComponent implements ModuleComponent {
    private Project project;

    public EazeStormModuleComponent(Module module) {
        this.project =   module.getProject();
    }

    public void initComponent() {
        // TODO: insert component initialization logic here
    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    @NotNull
    public String getComponentName() {
        return "ru.eaze.EazeStormModuleComponent";
    }

    public void projectOpened() {
        // called when project is opened
    }

    public void projectClosed() {
        // called when project is being closed
    }

    public void moduleAdded() {

        // Invoked when the module corresponding to this component instance has been completely
        // loaded and added to the project.
    }
}
