package ru.eaze.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.eaze.domain.EazeAction;
import ru.eaze.domain.EazePackage;
import ru.eaze.domain.EazeProjectStructure;

/**
 * Reference to action declaration in actions.xml
 */
public class EazeActionReference extends PsiReferenceBase<PsiElement> {

    private final String actionName;

    public EazeActionReference(@NotNull String actionName, @NotNull PsiElement element, @NotNull TextRange textRange) {
        super(element, textRange);
        this.actionName = actionName;
    }

    @Nullable
    public PsiElement resolve() {
        EazeProjectStructure structure = EazeProjectStructure.forProject(this.getElement().getProject());
        if (structure == null) {
            return null;
        }

        EazeAction action = structure.getActionByFullName(actionName);
        if (action != null) {
            XmlTag actionTag = action.getXmlTag();
            XmlAttribute attr = actionTag.getAttribute("name");
            if (attr != null) {
                return attr.getValueElement();
            }
            return actionTag;

        }

        return null;
    }

    @NotNull
    public String getCanonicalText() {
        return actionName;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        EazeProjectStructure structure = EazeProjectStructure.forProject(this.getElement().getProject());
        if (structure == null) {
            return new Object[0];
        }

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

    @Override
    public String toString() {
        return this.getCanonicalText();
    }
}
