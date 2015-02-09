package ru.eaze.locale.reference;

import com.intellij.openapi.util.Iconable;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.model.gotosymbol.GoToSymbolProvider.BaseNavigationItem;
import ru.eaze.locale.EazeLocaleDeclaration;

public class EazeLocaleNavigationElement extends BaseNavigationItem {

    private final EazeLocaleDeclaration declaration;
    private final XmlTag reference;
    private final String text;

    public EazeLocaleNavigationElement(EazeLocaleDeclaration declaration, XmlTag reference, String text) {
        super(reference, text, reference.getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY));
        this.declaration = declaration;
        this.reference = reference;
        this.text = text;
    }

    public EazeLocaleDeclaration getDeclaration() {
        return declaration;
    }

    public XmlTag getTag() {
        return reference;
    }

    @Override
    public String getName() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }
}
