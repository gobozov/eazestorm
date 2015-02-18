package ru.eaze.reference;

import com.intellij.openapi.util.TextRange;
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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Reference provider for pages.xml
 */
public class EazePagesReferenceProvider extends PsiReferenceProvider {

    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        EazeProjectStructure structure = EazeProjectStructure.forProject(element.getProject());
        PsiFile file = element.getContainingFile();
        if (structure == null || file == null || !file.isValid()
                || !(structure.isPagesConfigFile(file) || structure.isPagesConfigFile(file.getOriginalFile().getVirtualFile()))) {
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
        XmlText[] textElements = tag.getValue().getTextElements();
        if (textElements.length > 0) {
            XmlText text = textElements[0];
            int offset = text.getStartOffsetInParent();
            if (tag.getName().equals("template")) {
                return new PsiReference[] { new EazeUriReference(tag, new TextRange(offset, offset + text.getTextLength())) };
            }
            if (tag.getName().equals("actions") || tag.getName().equals("action")) {
                return  getActionReferences(text.getText(), tag, offset);
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }

    private PsiReference[] getReferencesByElement(@NotNull XmlAttributeValue attrValue) {
        if (attrValue.getParent() instanceof  XmlAttribute) {
            XmlAttribute attr = (XmlAttribute) attrValue.getParent();
            if (attr.getName().equals("shutdown") || attr.getName().equals("boot")) {
                int offset = attrValue.getText().indexOf(attrValue.getValue());
                return getActionReferences(attrValue.getValue(), attrValue, offset);
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }

    private PsiReference[] getActionReferences(String actionsString, PsiElement element, int startOffset) {
        if (actionsString.isEmpty()) {
            return PsiReference.EMPTY_ARRAY;
        }
        Collection<PsiReference> refs = new ArrayList<PsiReference>();
        String[] actions = actionsString.split(",");
        for (int i = 0, index = 0; i < actions.length; i++) {
            String actionName = actions[i].trim();
            if (!actionName.isEmpty()) {
                int start = startOffset + actionsString.indexOf(actionName, index);
                int end = start + actionName.length();
                refs.add(new EazeActionReference(actionName, element, new TextRange(start, end)));
            }
            index += actions[i].length() + 1;
        }
        return refs.toArray(new PsiReference[refs.size()]);
    }
}
