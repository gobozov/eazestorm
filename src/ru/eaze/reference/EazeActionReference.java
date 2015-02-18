package ru.eaze.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.eaze.domain.EazeAction;
import ru.eaze.domain.EazeProjectStructure;

import java.util.ArrayList;
import java.util.Collection;

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
            return action.getNavigationElement();
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
        EazeProjectStructure structure = EazeProjectStructure.forProject(getElement().getProject());
        if (structure != null) {
            Collection<String> names = structure.getAvailableActionNames();
            Collection<String> variants = new ArrayList<String>();
            for (String name : names) {
                if (name.startsWith(actionName)) {
                    int index = name.indexOf(".", actionName.length());
                    if (index > 0) {
                        variants.add(name.substring(0, index));
                    } else {
                        variants.add(name);
                    }
                }
            }
            return variants.toArray(new Object[variants.size()]);
        }
        return new Object[0];
    }

    @Override
    public boolean isReferenceTo(PsiElement element) {
        return resolve() == element;
    }

    @Override
    public String toString() {
        return this.getCanonicalText();
    }
}
