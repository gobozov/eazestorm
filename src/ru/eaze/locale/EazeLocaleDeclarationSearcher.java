package ru.eaze.locale;

import com.intellij.openapi.util.TextRange;
import com.intellij.pom.PomDeclarationSearcher;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.Consumer;
import com.jetbrains.php.lang.psi.elements.*;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EazeLocaleDeclarationSearcher extends PomDeclarationSearcher {

    private static final String LOCALE_LOADER_FQN = "\\LocaleLoader";
    private static final String TRANSLATE_FQN = "\\Translate";
    private static final String T_FQN = "\\T";

    private static  final Pattern LANG_PATTERN = Pattern.compile("\\{lang:(.+)\\}");

    @Override
    public void findDeclarationsAt(PsiElement element, int offsetInElement, Consumer<PomTarget> consumer) {
        PomTarget declaration = findDeclaration(element);
        if (declaration != null) {
            consumer.consume(declaration);
        }
    }

    @Nullable
    public static EazeLocaleDeclaration findDeclaration(PsiElement element) {
        if (element instanceof EazeLocaleDeclaration) {
            return (EazeLocaleDeclaration)element;
        }
        if (element instanceof StringLiteralExpression) {
            return findDeclaration((StringLiteralExpression) element);
        }
        if (element instanceof XmlToken) {
            return findDeclaration((XmlToken)element);
        }
        return null;
    }

    private static EazeLocaleDeclaration findDeclaration(StringLiteralExpression element) {
        if (isTranslateCall(element)) {
            return new EazeLocaleDeclaration(element, element.getValueRange());
        }
        if (isLegalLocaleKeyLiteral(element)) {
            return new EazeLocaleDeclaration(element, element.getValueRange(), true);
        }
        return null;
    }

    private static EazeLocaleDeclaration findDeclaration(XmlToken element) {
        if (element.getTokenType() == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN || element.getTokenType() == XmlTokenType.XML_DATA_CHARACTERS) {
            Matcher matcher = LANG_PATTERN.matcher(element.getText());
            if (matcher.matches() && matcher.groupCount() > 0) {
                TextRange range = new TextRange(matcher.start(1), matcher.end(1));
                return new EazeLocaleDeclaration(element, range);
            }
        }
        return null;
    }

    private static boolean isLegalLocaleKeyLiteral(StringLiteralExpression element) {
        if (element.getContext() instanceof AssignmentExpression) {
            return  EazeLocaleUtil.isValidKey(element.getContents());
        }
        return false;
    }

    private static boolean isTranslateCall(StringLiteralExpression element) {
        if (element.getContext() instanceof ParameterList) {
            ParameterList paramList = (ParameterList) element.getContext();
            return isLocaleLoaderTranslateCall(paramList.getContext())
                    || isTFunctionCall(paramList.getContext());
        }
        return false;
    }

    private static boolean isLocaleLoaderTranslateCall(PsiElement element) {
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

    private static boolean isTFunctionCall(PsiElement element) {
        if(element instanceof FunctionReference) {
            FunctionReference funcRef = (FunctionReference) element;
            return T_FQN.equals(funcRef.getFQN());
        }
        return false;
    }
}
