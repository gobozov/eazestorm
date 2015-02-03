package ru.eaze.locale.reference;

import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.model.gotosymbol.GoToSymbolProvider.BaseNavigationItem;

public class EazeLocaleNavigationElement extends BaseNavigationItem {

    private final PsiElement element;
    private final XmlTag reference;
    private final String text;

    public EazeLocaleNavigationElement(PsiElement element, XmlTag reference, String text) {
        super(reference, text, reference.getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY));
        this.element = element;
        this.reference = reference;
        this.text = text;
    }

    public PsiElement getElement() {
        return element;
    }

    public XmlTag getTag() {
        return reference;
    }

    @Override
    public String toString() {
        return text;
    }
}
