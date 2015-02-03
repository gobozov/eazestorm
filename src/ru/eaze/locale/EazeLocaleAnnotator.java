package ru.eaze.locale;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import ru.eaze.domain.EazeProjectStructure;

import java.util.Collection;

public class EazeLocaleAnnotator implements Annotator {

    private static final String ERROR_MESSAGE = "Missing Localization";
    private static final String WARNING_MESSAGE = "Partially Missing Localization";

    /**
     * Annotates the specified PSI element.
     * It is guaranteed to be executed in non-reentrant fashion.
     * I.e there will be no call of this method for this instance before previous call get completed.
     * Multiple instances of the annotator might exist simultaneously, though.
     *
     * @param element to annotate.
     * @param holder  the container which receives annotations created by the plugin.
     */
    @Override
    public void annotate(PsiElement element, AnnotationHolder holder) {
        EazeLocaleDeclaration declaration = EazeLocaleDeclarationSearcher.findDeclaration(element);
        if (declaration != null) {
            Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance().getContainingFiles(EazeLocaleKeyIndex.NAME, declaration.getValue(), GlobalSearchScope.projectScope(declaration.getProject()));
            Collection<VirtualFile> localeFiles = null;
            EazeProjectStructure structure = EazeProjectStructure.forProject(declaration.getProject());
            if (structure != null) {
                localeFiles = structure.getLocaleFiles();
            }

            if (containingFiles.isEmpty()) {
                Annotation annotation = holder.createErrorAnnotation(declaration.getValueTextRange(), ERROR_MESSAGE);
                if (localeFiles != null && !localeFiles.isEmpty()) {
                    for (VirtualFile file : localeFiles) {
                        //TODO QuickFix intention registration
                    }
                }
                return;
            }

            if (localeFiles != null && !localeFiles.isEmpty() && localeFiles.size() > containingFiles.size()) {
                Annotation annotation = holder.createWeakWarningAnnotation(declaration.getValueTextRange(), WARNING_MESSAGE);
                localeFiles.removeAll(containingFiles);
                for (VirtualFile file : localeFiles) {
                    //TODO QuickFix intention registration
                }
                return;
            }

            //TODO ChangeLocalization intention registration
        }
    }
}
