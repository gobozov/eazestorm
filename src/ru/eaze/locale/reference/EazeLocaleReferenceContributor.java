package ru.eaze.locale.reference;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;
import ru.eaze.locale.EazeLocaleDeclaration;

public class EazeLocaleReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        PsiReferenceProvider provider = new EazeLocaleReferenceProvider();
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(EazeLocaleDeclaration.class), provider);
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(StringLiteralExpression.class), provider);
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(XmlToken.class), provider);
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(XmlAttributeValue.class), provider);
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(XmlTag.class), provider);
    }
}
