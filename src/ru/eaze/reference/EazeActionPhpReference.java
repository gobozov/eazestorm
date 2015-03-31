package ru.eaze.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reference to action php file
 */
public class EazeActionPhpReference extends PsiReferenceBase<PsiElement> {

    private final String actionPath;

    public EazeActionPhpReference(@NotNull String actionPath, @NotNull PsiElement element, @NotNull TextRange textRange) {
        super(element, textRange);
        this.actionPath = actionPath;
    }

    @Nullable
    public PsiElement resolve() {
        if (actionPath.isEmpty()) {
            return null;
        }
        VirtualFile file = this.getElement().getProject().getBaseDir().findFileByRelativePath(actionPath);
        if (file != null) {
            return PsiManager.getInstance(this.getElement().getProject()).findFile(file);
        }
        return null;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }

    @Override
    public String toString() {
        return this.getCanonicalText();
    }
}
