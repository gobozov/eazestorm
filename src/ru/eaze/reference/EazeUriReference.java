package ru.eaze.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.eaze.domain.EazeProjectStructure;
import ru.eaze.domain.EazeSite;

public class EazeUriReference extends PsiReferenceBase<PsiElement> {

    public EazeUriReference(@NotNull PsiElement element, @NotNull TextRange textRange) {
        super(element, textRange);
    }

    @Nullable
    public PsiElement resolve() {
        EazeProjectStructure structure = EazeProjectStructure.forProject(this.getElement().getProject());
        if (structure == null) {
            return null;
        }

        final EazeSite.Host firstHost = structure.getFirstHost();   //TODO: do we really need the first host?
        final VirtualFile webDir = structure.getWebDir();
        if (firstHost == null) {
            return null;
        }

        String str = firstHost.translateEazePath(getValue());
        if (str == null) {
            return null;
        }

        VirtualFile targetFile = webDir.findFileByRelativePath(str);
        if (targetFile != null) {
            return PsiManager.getInstance(this.getElement().getProject()).findFile(targetFile);
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
