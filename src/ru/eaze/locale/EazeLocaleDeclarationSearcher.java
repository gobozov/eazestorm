package ru.eaze.locale;

import com.intellij.openapi.util.TextRange;
import com.intellij.pom.PomDeclarationSearcher;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.xml.*;
import com.intellij.util.Consumer;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EazeLocaleDeclarationSearcher extends PomDeclarationSearcher {

    private static final String LOCALE_LOADER_CLASS_NAME = "LocaleLoader";
    private static final String TRANSLATE_METHOD_NAME = "Translate";
    private static final String T_FUNCTION = "T";

    private static  final Pattern LANG_PATTERN = Pattern.compile(".*\\{lang:([^&].*)\\}.*");

    @Override
    public void findDeclarationsAt(@NotNull PsiElement element, int offsetInElement, Consumer<PomTarget> consumer) {
        PomTarget declaration = findDeclaration(element);
        if (declaration != null) {
            consumer.consume(declaration);
        }
    }

    @Nullable
    public static EazeLocaleDeclaration findDeclaration(PsiElement element) {
        return findDeclaration(element, true);
    }

    @Nullable
    public static EazeLocaleDeclaration findDeclaration(PsiElement element, boolean checkScope) {
        if (checkScope && !EazeLocaleUtil.inScope(element)) {
            return null;
        }
        if (element instanceof EazeLocaleDeclaration) {
            return (EazeLocaleDeclaration)element;
        }
        if (element instanceof StringLiteralExpression) {
            return findDeclaration((StringLiteralExpression)element);
        }
        if (element instanceof LeafPsiElement && element.getContext() instanceof StringLiteralExpression) {
            return findDeclaration((StringLiteralExpression)element.getContext());
        }
        if (element instanceof XmlElement) {
            return findDeclaration((XmlElement)element);
        }
        return null;
    }

    private static EazeLocaleDeclaration findDeclaration(StringLiteralExpression element) {
        if (isTranslateCall(element)) {
            TextRange keyRange = element.getValueRange();
            if (!EazeLocaleUtil.isValidKey(element.getContents())) {
                int keyLength = EazeLocaleUtil.findKeyInString(element.getContents()).length();
                if (keyLength == 0) {
                    return null;
                }
                int start = element.getValueRange().getStartOffset();
                int end = start + keyLength;
                keyRange = new TextRange(start, end);
            }
            return new EazeLocaleDeclaration(element, keyRange);
        }
        if (isLegalLocaleKeyLiteral(element)) {
            return new EazeLocaleDeclaration(element, element.getValueRange(), true);
        }
        return null;
    }

    private static EazeLocaleDeclaration findDeclaration(XmlElement element) {
        if (element.getNode().getElementType() == XmlTokenType.XML_DATA_CHARACTERS
                || element.getNode().getElementType() == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN
                || element.getNode().getElementType() == XmlElementType.XML_ATTRIBUTE_VALUE) {

            Matcher matcher = LANG_PATTERN.matcher(element.getText());
            if (matcher.matches() && matcher.groupCount() > 0) {
                TextRange range =  new TextRange(matcher.start(1), matcher.end(1));
                return new EazeLocaleDeclaration(element, range);
            }
        }
        return null;
    }

    private static boolean isLegalLocaleKeyLiteral(StringLiteralExpression element) {
        return element.getContext() instanceof AssignmentExpression && EazeLocaleUtil.deepIsValidKey(element.getContents(), element.getProject());
    }

    private static boolean isTranslateCall(StringLiteralExpression element) {
        if (element.getContext() instanceof ParameterList) {
            ParameterList paramList = (ParameterList) element.getContext();
            boolean translate = isLocaleLoaderTranslateCall(paramList.getContext()) || isTFunctionCall(paramList.getContext());
            if (translate) {
                PsiElement[] params = paramList.getParameters();
                return params.length > 0 && params[0].equals(element);
            }
        }
        return false;
    }

    private static boolean isLocaleLoaderTranslateCall(PsiElement element) {
        if(element instanceof MethodReference) {
            MethodReference methodRef = (MethodReference) element;
            if (methodRef.getClassReference() instanceof ClassReference) {
                ClassReference classRef = (ClassReference) methodRef.getClassReference();
                return LOCALE_LOADER_CLASS_NAME.equals(classRef.getName())
                        && TRANSLATE_METHOD_NAME.equals(methodRef.getName());
            }
        }
        return false;
    }

    private static boolean isTFunctionCall(PsiElement element) {
        if(element instanceof FunctionReference) {
            FunctionReference funcRef = (FunctionReference) element;
            return T_FUNCTION.equals(funcRef.getName());
        }
        return false;
    }
}
