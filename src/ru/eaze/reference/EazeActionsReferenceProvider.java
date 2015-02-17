package ru.eaze.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import ru.eaze.domain.EazeProjectStructure;

/**
 * Refrence provider for actions.xml
 */
public class EazeActionsReferenceProvider extends PsiReferenceProvider {

    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull final ProcessingContext context) {
        EazeProjectStructure structure = EazeProjectStructure.forProject(element.getProject());
        PsiFile file = element.getContainingFile();
        if (structure == null || file == null || !file.isValid() || !structure.isActionsConfigFile(file.getVirtualFile())) {
            return PsiReference.EMPTY_ARRAY;
        }
        if (element instanceof XmlTag) {
            return getReferencesByElement((XmlTag) element);
        }
        if (element instanceof XmlAttributeValue) {
            return getReferencesByElement((XmlAttributeValue) element);
        }
        return PsiReference.EMPTY_ARRAY;
    }

    private PsiReference[] getReferencesByElement(@NotNull XmlTag tag) {
        if (tag.getName().equals("path") && !tag.getValue().getTrimmedText().isEmpty() && tag.getParentTag() != null && tag.getParentTag().getName().equals("action")) {
            VirtualFile actionFile = EazeProjectStructure.getFileByActionTag(tag.getParentTag());
            String actionPath = actionFile == null ? "" : actionFile.getPath().substring(tag.getProject().getBasePath().length());
            XmlText text = tag.getValue().getTextElements()[0];
            int offset = text.getStartOffsetInParent();
            return new PsiReference[] { new EazeActionPhpReference(actionPath, tag, new TextRange(offset, offset + text.getTextLength())) };
        }
        return PsiReference.EMPTY_ARRAY;
    }

    private PsiReference[] getReferencesByElement(@NotNull XmlAttributeValue attrValue) {
        if (attrValue.getParent() instanceof XmlAttribute) {
            XmlAttribute attr = (XmlAttribute) attrValue.getParent();
            if (attr.getName().equals("name")) {
                XmlTag tag = attr.getParent();
                if (tag.getName().equals("action")) {
                    VirtualFile actionFile = EazeProjectStructure.getFileByActionTag(tag);
                    if (actionFile != null) {  // чтобы не подсвечивало красным сразу две строчки
                        String actionPath = actionFile.getPath().substring(attrValue.getProject().getBasePath().length());
                        int start = attrValue.getText().indexOf(attrValue.getValue());
                        int end = start + attrValue.getValue().length();
                        PsiReference ref = new EazeActionPhpReference(actionPath, attrValue, new TextRange(start, end));
                        return new PsiReference[]{ref};
                    }
                }
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }
}
