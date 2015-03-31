package ru.eaze.locale.completion;

import com.intellij.codeInsight.completion.CompletionConfidence;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ThreeState;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.AssignmentExpression;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;
import ru.eaze.locale.EazeLocaleUtil;

public class EazeLocaleCompletionConfidence extends CompletionConfidence {
    @NotNull
    @Override
    public ThreeState shouldSkipAutopopup(@NotNull PsiElement contextElement, @NotNull PsiFile psiFile, int offset) {
        if(!(psiFile instanceof PhpFile || psiFile instanceof XmlFile)) {
            return ThreeState.UNSURE;
        }

        if (!EazeLocaleUtil.inScope(contextElement)) {
            return ThreeState.UNSURE;
        }

        if (contextElement instanceof XmlToken) {
            IElementType type = ((XmlToken)contextElement).getTokenType();
            if (type == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN || type == XmlTokenType.XML_DATA_CHARACTERS) {
                return ThreeState.NO;
            } else {
                return ThreeState.UNSURE;
            }
        }

        PsiElement context = contextElement.getContext();
        if(!(context instanceof StringLiteralExpression)) {
            return ThreeState.UNSURE;
        }

        if(context.getParent() instanceof AssignmentExpression) {
            return ThreeState.NO;
        }

        PsiElement stringContext = context.getContext();
        if(stringContext instanceof ParameterList) {
            return ThreeState.NO;
        }
        return ThreeState.UNSURE;
    }
}
