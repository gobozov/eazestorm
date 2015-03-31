package ru.eaze.locale.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

public class EazeLocaleTagReference extends PsiReferenceBase<XmlTag> {

    private final String tagKey;

    public EazeLocaleTagReference(@NotNull String tagKey, @NotNull XmlTag tag, @NotNull TextRange textRange) {
        super(tag, textRange);
        this.tagKey = tagKey;
    }

    @NotNull
    @Override
    public PsiElement resolve() {
        return new EazeLocaleNavigationElement(this.getElement(), tagKey);
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }
}
