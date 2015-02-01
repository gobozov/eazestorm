package ru.eaze.locale;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EazeLocaleReferenceProvider extends PsiReferenceProvider {

    private static final String LOCALE_LOADER_FQN = "\\LocaleLoader";
    private static final String TRANSLATE_FQN = "\\Translate";
    private static final String T_FQN = "\\T";

    private static  final Pattern LANG_PATTERN = Pattern.compile("\\{lang:(.+)\\}");

    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (element instanceof StringLiteralExpression) {
            return getReferencesByElement((StringLiteralExpression) element, context);
        }
        if (element instanceof XmlToken) {
            return getReferencesByElement((XmlToken) element, context);
        }
        return PsiReference.EMPTY_ARRAY;
    }

    @NotNull
    public PsiReference[] getReferencesByElement(@NotNull StringLiteralExpression element, @NotNull ProcessingContext context) {
        if (element.getContext() instanceof ParameterList) {
            ParameterList paramList = (ParameterList) element.getContext();
            if (isTranslateCall(paramList.getContext())) {
                TextRange range = element.getValueRange();
                return new PsiReference[] { new EazeLocaleReference(element, range) };
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }

    private boolean isTranslateCall(PsiElement element) {
        return isLocaleLoaderTranslateCall(element)
                || isTFunctionCall(element);
    }

    private boolean isLocaleLoaderTranslateCall(PsiElement element) {
        if(element instanceof MethodReference) {
            MethodReference methodRef = (MethodReference) element;
            if (methodRef.getClassReference() instanceof ClassReference) {
                ClassReference classRef = (ClassReference) methodRef.getClassReference();
                return LOCALE_LOADER_FQN.equals(classRef.getFQN())
                        && TRANSLATE_FQN.equals(methodRef.getFQN());
            }
        }
        return false;
    }

    private boolean isTFunctionCall(PsiElement element) {
        if(element instanceof FunctionReference) {
            FunctionReference funcRef = (FunctionReference) element;
            return T_FQN.equals(funcRef.getFQN());
        }
        return false;
    }

    @NotNull
    public PsiReference[] getReferencesByElement(@NotNull XmlToken element, @NotNull ProcessingContext context) {
        if (element.getTokenType() == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN || element.getTokenType() == XmlTokenType.XML_DATA_CHARACTERS) {
            Matcher matcher = LANG_PATTERN.matcher(element.getText());
            if (matcher.matches() && matcher.groupCount() > 0) {
                TextRange range = new TextRange(matcher.start(1), matcher.end(1));
                return new PsiReference[] { new EazeLocaleReference(element, range) };
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }
}
