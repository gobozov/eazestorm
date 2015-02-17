package ru.eaze.component;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;

public class EazeStormProjectComponent implements ProjectComponent {
    private Project project;

    public EazeStormProjectComponent(Project project) {
        this.project = project;
    }

    @Override
    public void initComponent() {
        // TODO: insert component initialization logic here
    }

    @Override
    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    @Override
    @NotNull
    public String getComponentName() {
        return "ru.eaze.EazeStormProjectComponent";
    }

    public void projectOpened() {
           PsiDocumentManager.getInstance( project ).addListener(new MyDocumentManagerListener());
    }

    @Override
    public void projectClosed() {
        // called when project is being closed
    }
}
