package ru.eaze.locale.refactoring;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlFile;
import com.intellij.refactoring.rename.RenameHandler;
import com.jetbrains.php.lang.psi.PhpFile;
import org.jetbrains.annotations.NotNull;
import ru.eaze.locale.EazeLocaleDeclarationSearcher;
import ru.eaze.locale.reference.EazeLocaleNavigationElement;
import ru.eaze.locale.reference.EazeLocaleTagReference;

public class EazeLocaleRenameHandler implements RenameHandler {

    private static final String HANDLER_NAME = "Rename Eaze localization key";

    @Override
    public boolean isAvailableOnDataContext(DataContext dataContext) {
        return getDeclaration(dataContext) != null;
    }

    @Override
    public boolean isRenaming(DataContext dataContext) {
        return this.isAvailableOnDataContext(dataContext);
    }

    /**
     * Invokes refactoring action from editor. The refactoring obtains
     * all data from editor selection.
     *
     * @param project     the project in which the refactoring is invoked.
     * @param editor      editor that refactoring is invoked in
     * @param file        file should correspond to <code>editor</code>
     * @param dataContext can be null for some but not all of refactoring action handlers
     */
    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
        invoke(project, dataContext);
    }

    /**
     * Invokes refactoring action from elsewhere (not from editor). Some refactorings
     * do not implement this method.
     *
     * @param project     the project in which the refactoring is invoked.
     * @param elements    list of elements that refactoring should work on. Refactoring-dependent.
     * @param dataContext can be null for some but not all of refactoring action handlers
     */
    @Override
    public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, DataContext dataContext) {
        invoke(project, dataContext);
    }

    private void invoke(@NotNull final Project project, DataContext dataContext) {
        final PsiNamedElement declaration = getDeclaration(dataContext);
        if (declaration != null) {
            String message = String.format("Rename Localization Key %s and its usages to", declaration.getName());
            String newName = Messages.showInputDialog(project, message, "Rename Localization Key", null, declaration.getName(), new InputValidator() {
                @Override
                public boolean checkInput(String inputString) {
                    return EazeLocaleRenameUtil.canRename(project, declaration.getName(), inputString);
                }

                @Override
                public boolean canClose(String inputString) {
                    return checkInput(inputString);
                }
            });
            if (EazeLocaleRenameUtil.canRename(project, declaration.getName(), newName)) {
                EazeLocaleRenameProcessor rename = new EazeLocaleRenameProcessor(declaration, newName);
                rename.setPreviewUsages(true);
                rename.run();
            }
        }
    }

    private static PsiNamedElement getDeclaration(DataContext context) {
        if (context != null) {
            final Editor editor = CommonDataKeys.EDITOR.getData(context);
            if (editor != null) {
                final int offset = editor.getCaretModel().getOffset();
                final PsiFile file = CommonDataKeys.PSI_FILE.getData(context);
                PsiElement element = file == null ? null : file.getViewProvider().findElementAt(offset);
                if (element != null) {
                    if (file instanceof PhpFile) {
                        return EazeLocaleDeclarationSearcher.findDeclaration(element);
                    }
                    if (file instanceof XmlFile) {
                        PsiReference[] references = PsiReference.EMPTY_ARRAY;
                        while (element.getNode().getElementType() != XmlElementType.XML_TAG && element.getParent() != null) {
                            element = element.getParent();
                        }
                        if (element.getNode().getElementType() == XmlElementType.XML_TAG) {
                            references = element.getReferences();
                        }
                        for (PsiReference reference : references) {
                            if (reference instanceof EazeLocaleTagReference) {
                                return (EazeLocaleNavigationElement) reference.resolve();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return HANDLER_NAME;
    }
}
