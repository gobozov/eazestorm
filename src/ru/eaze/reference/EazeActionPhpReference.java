package ru.eaze.reference;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.Nullable;
import ru.eaze.domain.EazeProjectStructure;

/**
 * Created by IntelliJ IDEA.
 * User: user
 * Date: 27.01.12
 * Time: 1:38
 * To change this template use File | Settings | File Templates.
 */
public class EazeActionPhpReference extends MyXmlTagReference {
    private String actionName;
    private XmlTag actionTag;

    public EazeActionPhpReference(XmlTag actionTag, String actionName, PsiElement element, TextRange textRange, EazeProjectStructure structure, Project project) {
        super(element, textRange, structure, project);
        this.actionName = actionName;
        this.actionTag = actionTag;
    }

    @Nullable
    public PsiElement resolve() {
        if (structure == null) {
            return null;
        }

        //   XmlTag actionTag = (XmlTag)element;
        VirtualFile file = EazeProjectStructure.GetFileByActionTag(actionTag);
        if (file != null) {
            return PsiManager.getInstance(project).findFile(file);
        }

        return null;
    }

    public String getCanonicalText() {
        return actionName;
    }
}
