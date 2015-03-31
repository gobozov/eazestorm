package ru.eaze.locale.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import ru.eaze.locale.EazeLocaleDeclaration;
import ru.eaze.locale.EazeLocaleDeclarationSearcher;
import ru.eaze.locale.EazeLocaleUtil;

import java.util.ArrayList;
import java.util.List;

public class EazeLocaleReferenceProvider extends PsiReferenceProvider {

    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        EazeLocaleDeclaration declaration = EazeLocaleDeclarationSearcher.findDeclaration(element);
        if (declaration != null) {
            return new PsiReference[] { new EazeLocaleReference(declaration, declaration.getValueRange()) };
        }
        if (element instanceof XmlTag) {
            XmlTag tag = (XmlTag) element;
            String tagKey = EazeLocaleUtil.extractTagKey(tag);
            if (tagKey != null) {
                List<PsiReference> references = new ArrayList<PsiReference>();
                if (EazeLocaleUtil.isValueTag(tag)) {
                    references.add(new EazeLocaleTagReference(tagKey, tag, new TextRange(0, tag.getTextLength())));
                }
                for (PsiElement child : tag.getChildren()) {
                    if (child.getNode().getElementType() == XmlTokenType.XML_NAME) {
                        int start = child.getStartOffsetInParent();
                        int end = start + child.getTextLength();
                        references.add(new EazeLocaleTagReference(tagKey, tag, new TextRange(start, end)));
                    }
                }
                return references.toArray(new PsiReference[references.size()]);
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }
}
