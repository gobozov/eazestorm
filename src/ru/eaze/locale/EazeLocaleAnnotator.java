package ru.eaze.locale;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;
import ru.eaze.domain.EazeProjectStructure;
import ru.eaze.locale.action.CreateLocaleAction;
import ru.eaze.locale.action.EditLocaleAction;

import java.util.ArrayList;
import java.util.Collection;

public class EazeLocaleAnnotator implements Annotator {

    private static final String MISSING_MESSAGE = "Missing Localization Key";
    private static final String SOFT_MISSING_MESSAGE = "Probable Missing Localization Key";
    private static final String PARTIAL_MISSING_MESSAGE = "Partially Missing Localization Key";
    private static final String INVALID_MESSAGE = "Invalid Localization Key";
    private static final String NO_FILES_MESSAGE = "Missing Localization Files";
    private static final String DEFAULT_INFO_MESSAGE = "Localization Key";

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
        if (element instanceof LeafPsiElement && element.getContext() instanceof StringLiteralExpression) {
            return; //prevents duplicate annotations for php string literals
        }
        EazeLocaleDeclaration declaration = EazeLocaleDeclarationSearcher.findDeclaration(element);
        if (declaration != null) {
            //should not happen but just in case (blame declaration searcher)
            if (!EazeLocaleUtil.isValidKey(declaration.getValue())) {
                if (!declaration.isSoft()) {
                    holder.createErrorAnnotation(declaration.getValueTextRange(), INVALID_MESSAGE);
                }
                return;
            }

            EazeProjectStructure structure = EazeProjectStructure.forProject(declaration.getProject());
            if (structure == null) {
                return;
            }
            Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance().getContainingFiles(EazeLocaleKeyIndex.NAME, declaration.getValue(), structure.projectScope());
            Collection<VirtualFile> localeFiles = EazeLocaleUtil.getLocaleFiles(declaration.getProject());
            if (localeFiles.isEmpty()) {
                if (!declaration.isSoft()) {
                    holder.createErrorAnnotation(declaration.getValueTextRange(), NO_FILES_MESSAGE);
                    return;
                }
            }

            //missing or incomplete key
            if (containingFiles.isEmpty()) {
                Collection<VirtualFile> tagFiles = new ArrayList<VirtualFile>();
                for (VirtualFile file : localeFiles) {
                    XmlTag tag = EazeLocaleUtil.findTagForKey(declaration.getProject(), file, declaration.getValue());
                    if (tag != null) {
                        tagFiles.add(file);
                    }
                }
                if (tagFiles.isEmpty()) {
                    Annotation annotation = declaration.isSoft() ?
                            holder.createWeakWarningAnnotation(declaration.getValueTextRange(), SOFT_MISSING_MESSAGE) :
                            holder.createErrorAnnotation(declaration.getValueTextRange(), MISSING_MESSAGE);
                    for (VirtualFile file : localeFiles) {
                        annotation.registerFix(new CreateLocaleAction(declaration.getValue(), file));
                    }
                } else if (tagFiles.size() < localeFiles.size()) {
                    Annotation annotation = declaration.isSoft() ?
                            holder.createWeakWarningAnnotation(declaration.getValueTextRange(), SOFT_MISSING_MESSAGE) :
                            holder.createWeakWarningAnnotation(declaration.getValueTextRange(), PARTIAL_MISSING_MESSAGE);
                    localeFiles.removeAll(tagFiles);
                    for (VirtualFile file : localeFiles) {
                        annotation.registerFix(new CreateLocaleAction(declaration.getValue(), file));
                    }
                }
                return;
            }

            //key missing in some files (conflicts resolution not supported)
            if (localeFiles.size() > containingFiles.size()) {
                Annotation annotation = holder.createWeakWarningAnnotation(declaration.getValueTextRange(), PARTIAL_MISSING_MESSAGE);
                localeFiles.removeAll(containingFiles);
                for (VirtualFile file : localeFiles) {
                    annotation.registerFix(new CreateLocaleAction(declaration.getValue(), file));
                }
                registerEditActions(annotation, declaration.getValue(), containingFiles);
                return;
            }

            String message = EazeLocaleUtil.createTextForAnnotation(declaration.getValue(), declaration.getProject());
            message = message.isEmpty() ? DEFAULT_INFO_MESSAGE : message;
            Annotation annotation = holder.createInfoAnnotation(declaration.getValueTextRange(), message);
            registerEditActions(annotation, declaration.getValue(), containingFiles);
        }
    }

    private void registerEditActions(Annotation annotation, String key, Collection<VirtualFile> files) {
        for (VirtualFile file : files) {
            annotation.registerFix(new EditLocaleAction(key, file));
        }
    }
}
