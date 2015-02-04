package ru.eaze.locale;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;
import ru.eaze.domain.EazeProjectStructure;
import ru.eaze.locale.action.CreateLocaleIntentionAction;

import java.util.Collection;
import java.util.Collections;

public class EazeLocaleAnnotator implements Annotator {

    private static final String MISSING_MESSAGE = "Missing Localization";
    private static final String SOFT_MISSING_MESSAGE = "Probable Missing Localization";
    private static final String PARTIAL_MISSING_MESSAGE = "Partially Missing Localization";
    private static final String INVALID_MESSAGE = "Invalid Localization Key";
    private static final String INCOMPLETE_MESSAGE = "Incomplete Localization Key";
    private static final String NO_FILES_MESSAGES = "Missing Localization Files";

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
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        EazeLocaleDeclaration declaration = EazeLocaleDeclarationSearcher.findDeclaration(element);
        if (declaration != null) {
            if (!EazeLocaleUtil.isValidKey(declaration.getValue())) {
                if (!declaration.isSoft()) {
                    holder.createErrorAnnotation(declaration.getValueTextRange(), INVALID_MESSAGE);
                }
                return;
            }

            Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance().getContainingFiles(EazeLocaleKeyIndex.NAME, declaration.getValue(), GlobalSearchScope.projectScope(declaration.getProject()));
            Collection<VirtualFile> localeFiles = Collections.emptyList();
            EazeProjectStructure structure = EazeProjectStructure.forProject(declaration.getProject());
            if (structure != null) {
                localeFiles = structure.getLocaleFiles();
            }
            if (localeFiles.isEmpty()) {
                if (!declaration.isSoft()) {
                    holder.createErrorAnnotation(declaration.getValueTextRange(), NO_FILES_MESSAGES);
                }
            }

            if (containingFiles.isEmpty()) {
                XmlTag tag = null;
                for (VirtualFile file : localeFiles) {
                    tag = EazeLocaleUtil.findTagForKey(declaration.getProject(), file, declaration.getValue());
                }
                if (tag == null) {
                    Annotation annotation = declaration.isSoft() ?
                            holder.createWeakWarningAnnotation(declaration.getValueTextRange(), SOFT_MISSING_MESSAGE) :
                            holder.createErrorAnnotation(declaration.getValueTextRange(), MISSING_MESSAGE);
                    for (VirtualFile file : localeFiles) {
                        annotation.registerFix(new CreateLocaleIntentionAction(declaration.getValue(), file));
                    }
                } else if (!declaration.isSoft()) {
                    holder.createErrorAnnotation(declaration.getValueTextRange(), INCOMPLETE_MESSAGE);
                }
                return;
            }

            if (localeFiles.size() > containingFiles.size()) {
                Annotation annotation = holder.createWeakWarningAnnotation(declaration.getValueTextRange(), PARTIAL_MISSING_MESSAGE);
                localeFiles.removeAll(containingFiles);
                for (VirtualFile file : localeFiles) {
                    annotation.registerFix(new CreateLocaleIntentionAction(declaration.getValue(), file));
                }
                return;
            }

            //TODO ChangeLocalization intention registration
        }
    }
}
