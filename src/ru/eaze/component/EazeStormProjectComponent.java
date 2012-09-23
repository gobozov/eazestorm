package ru.eaze.component;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;
import ru.eaze.domain.EazeProjectStructure;

/**
 * Created by IntelliJ IDEA.
 * User: user
 * Date: 26.01.12
 * Time: 22:14
 */

public class EazeStormProjectComponent implements ProjectComponent {
    private Project project;
    private EazeProjectStructure projectStructure;

    public EazeStormProjectComponent(Project project) {
        this.project = project;
    }



    public Project getCurrentProject() {
        return project;
    }

    public void initComponent() {


        // TODO: insert component initialization logic here
    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    @NotNull
    public String getComponentName() {
        return "ru.eaze.EazeStormProjectComponent";
    }

    public void projectOpened() {
           PsiDocumentManager.getInstance( project ).addListener(new MyDocumentManagerListener());
    }

    public void projectClosed() {
        // called when project is being closed
    }
}
