package ru.eaze.reference;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlAttribute;
import org.jetbrains.annotations.Nullable;
import ru.eaze.domain.EazeAction;
import ru.eaze.domain.EazePackage;
import ru.eaze.domain.EazeProjectStructure;

/**
 * Created by IntelliJ IDEA.
 * User: user
 * Date: 27.01.12
 * Time: 0:20
 * To change this template use File | Settings | File Templates.
 */

public class EazeActionReference extends MyXmlTagReference {
    private String actionName;

    public EazeActionReference(String actionName, PsiElement element, TextRange textRange, EazeProjectStructure structure, Project project) {
        super(element, textRange, structure, project);
        this.actionName = actionName;
    }

    @Nullable
    public PsiElement resolve() {


        if (structure == null) {
            return null;
        }

        EazeAction action = structure.getActionByFullName(actionName);
        if (action != null) {
            XmlTag actionTag = action.getXmlTag();
            if (action != null) {
                XmlAttribute attr = actionTag.getAttribute("name");
                if (attr != null) {
                    return attr.getValueElement();
                }
                return actionTag;
            }

        }


        return null;
    }

    public String getCanonicalText() {
        return actionName;
        // return "lol";//getText();
    }

    @Override
    public Object[] getVariants() {
        String[] tokens = actionName.split("\\.");
        if (tokens.length > 1) {
            String packageName = tokens[0] + "." + tokens[1];
            EazePackage pack = structure.getPackageByName(packageName);
            if (pack != null) {
                return pack.getAvailableActionNames();
            }

        }
        return structure.getAvailablePackageNames();

    }
}
