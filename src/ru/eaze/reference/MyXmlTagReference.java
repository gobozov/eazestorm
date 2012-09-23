package ru.eaze.reference; /**
 * Created by IntelliJ IDEA.
 * User: user
 * Date: 26.01.12
 * Time: 0:56
 */

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
//import com.intellij.psi.impl.source.resolve.reference.ReferenceType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nullable;
import ru.eaze.domain.EazeProjectStructure;

public class MyXmlTagReference implements PsiReference {
    protected PsiElement element;
    protected TextRange textRange;
    protected EazeProjectStructure structure;
    protected Project project;

    public MyXmlTagReference(PsiElement element, TextRange textRange, EazeProjectStructure structure, Project project) {
        this.element = element;
        this.textRange = textRange;
        this.structure = structure;
        this.project = project;

        if (getCanonicalText() == null) {
            System.out.println("oops");
        }
        System.out.println(getCanonicalText() + " start= " + String.valueOf(textRange.getStartOffset()) + " length=" + String.valueOf(textRange.getLength()));
        // super(astNode);
    }

    @Override
    public String toString() {
        return getCanonicalText();
    }

    public PsiElement getElement() {
        return this.element;
    }

    public TextRange getRangeInElement() {
        return textRange;
    }

    @Nullable
    public PsiElement resolve() {
        /* TODO: rewrite using PomService.convertToPsi()
            It is way of saying that an XML tag is more
            than it seems to be, and it holds some model's element which can be
            referred to and searched for usages.
        */
        return null;
    }

    //@Override
    public String getCanonicalText() {
        String text = element.getText().substring(textRange.getStartOffset(), textRange.getEndOffset());
        return text;
    }

    public PsiElement handleElementRename(String newElementName)
            throws IncorrectOperationException {
        // TODO: Implement this method
        throw new IncorrectOperationException();
    }

    public PsiElement bindToElement(PsiElement element) throws IncorrectOperationException {
        // TODO: Implement this method
        throw new IncorrectOperationException();
    }

    public boolean isReferenceTo(PsiElement element) {
        return resolve() == element;
    }

    public Object[] getVariants() {
        // TODO: Implement this method
        return new Object[0];
    }

    public boolean isSoft() {
        return false;
    }


}
