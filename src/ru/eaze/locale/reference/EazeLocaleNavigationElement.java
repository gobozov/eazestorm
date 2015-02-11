package ru.eaze.locale.reference;

import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.model.gotosymbol.GoToSymbolProvider.BaseNavigationItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.eaze.locale.EazeLocaleDeclaration;
import ru.eaze.locale.refactoring.EazeLocaleRenameUtil;

public class EazeLocaleNavigationElement extends BaseNavigationItem {

    private final EazeLocaleDeclaration declaration;
    private final XmlTag reference;
    private final String name;

    public EazeLocaleNavigationElement(EazeLocaleDeclaration declaration, XmlTag reference, String name) {
        super(reference, name, reference.getIcon(Iconable.ICON_FLAG_READ_STATUS | Iconable.ICON_FLAG_VISIBILITY));
        this.declaration = declaration;
        this.reference = reference;
        this.name = name;
    }

    public EazeLocaleDeclaration getDeclaration() {
        return declaration;
    }

    public XmlTag getTag() {
        return reference;
    }

    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public PsiElement getNavigationElement() {
        return this;
    }

    @Nullable
    @Override
    public String getText() {
        return reference.getText();
    }

    @Override
    public int getTextLength() {
        return reference.getTextLength();
    }

    @Override
    public int getTextOffset() {
        return reference.getTextOffset();
    }

    @Nullable
    @Override
    public TextRange getTextRange() {
        return reference.getTextRange();
    }

    @Override
    public int getStartOffsetInParent() {
        return reference.getStartOffsetInParent();
    }

    @Override
    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        if (getName().equals(name)) {
            return this;
        }
        EazeLocaleDeclaration declaration = (EazeLocaleDeclaration)this.declaration.setName(name);
        XmlTag reference = EazeLocaleRenameUtil.renameKeyTag(this.reference, name);
        if (declaration != null && reference != null) {
            return new EazeLocaleNavigationElement(declaration, reference, declaration.getValue());
        }
        throw new IncorrectOperationException();
    }



    @Override
    public String toString() {
        return name;
    }
}
