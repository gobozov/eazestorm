package ru.eaze.reference;

import com.intellij.patterns.*;
import com.intellij.psi.*;
import com.intellij.psi.xml.*;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

public class EazeReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        EazePagesReferenceProvider pagesProvider = new EazePagesReferenceProvider();
        registrar.registerReferenceProvider(StandardPatterns.instanceOf(XmlTag.class), pagesProvider);
        registrar.registerReferenceProvider(StandardPatterns.instanceOf(XmlAttributeValue.class), pagesProvider);

        EazeActionsReferenceProvider provider = new EazeActionsReferenceProvider();
        registrar.registerReferenceProvider(StandardPatterns.instanceOf(XmlTag.class), provider);
        registrar.registerReferenceProvider(StandardPatterns.instanceOf(XmlAttributeValue.class), provider);

        EazeUriReferenceProvider uriProvider = new EazeUriReferenceProvider();
        registrar.registerReferenceProvider(StandardPatterns.instanceOf(XmlToken.class), uriProvider);
        registrar.registerReferenceProvider(StandardPatterns.instanceOf(StringLiteralExpression.class), uriProvider);
 }
}
