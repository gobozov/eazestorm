package ru.eaze.locale.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.eaze.locale.EazeLocaleDeclaration;
import ru.eaze.locale.EazeLocaleKeyIndex;

import java.util.Collection;

public class EazeLocaleReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private final EazeLocaleDeclaration declaration;

    public EazeLocaleReference(@NotNull EazeLocaleDeclaration declaration, @NotNull TextRange range) {
        super(declaration, range);
        this.declaration = declaration;
    }

    /**
     * Returns the results of resolving the reference.
     *
     * @param incompleteCode if true, the code in the context of which the reference is
     *                       being resolved is considered incomplete, and the method may return additional
     *                       invalid results.
     * @return the array of results for resolving the reference.
     */
    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        Collection<XmlTag> declarations = EazeLocaleKeyIndex.findKeyDeclarations(this.getElement().getProject(), this.getValue());
        ResolveResult[] results = new ResolveResult[declarations.size()];
        int i = 0;
        for (XmlTag tag : declarations) {
            results[i++] = new PsiElementResolveResult(new EazeLocaleNavigationElement(declaration, tag, declaration.getValue()));
        }
        return results;
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        ResolveResult[] results = multiResolve(false);
        return results.length > 0 ? results[0].getElement() : declaration;
    }

    /**
     * Returns the array of String, {@link com.intellij.psi.PsiElement} and/or {@link com.intellij.codeInsight.lookup.LookupElement}
     * instances representing all identifiers that are visible at the location of the reference. The contents
     * of the returned array is used to build the lookup list for basic code completion. (The list
     * of visible identifiers may not be filtered by the completion prefix string - the
     * filtering is performed later by IDEA core.)
     *
     * @return the array of available identifiers.
     */
    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }

    @Override
    public String toString() {
        return this.getCanonicalText();
    }
}
