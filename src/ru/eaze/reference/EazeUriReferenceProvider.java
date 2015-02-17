package ru.eaze.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;
import ru.eaze.domain.EazeProjectStructure;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reference provider for Eaze virtual URIs (except found in pages.xml)
 */
public class EazeUriReferenceProvider extends PsiReferenceProvider {

    private static  final Pattern INCREAL_PATTERN = Pattern.compile("\\{increal:(.+)\\}");

    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (element instanceof XmlToken) {
            return getReferencesByElement((XmlToken) element);
        }
        if (element instanceof StringLiteralExpression) {
            return getReferencesByElement((StringLiteralExpression) element);
        }
        return PsiReference.EMPTY_ARRAY;
    }

    private PsiReference[] getReferencesByElement(XmlToken token) {
        if (token.getTokenType() == XmlTokenType.XML_DATA_CHARACTERS) {
            Matcher matcher = INCREAL_PATTERN.matcher(token.getText());
            if (matcher.matches() && matcher.groupCount() > 0) {
                TextRange range = new TextRange(matcher.start(1), matcher.end(1));
                return new PsiReference[] { new EazeUriReference(token, range) };
            }
        }
        return PsiReference.EMPTY_ARRAY;
    }

    private PsiReference[] getReferencesByElement(StringLiteralExpression literal) {
        EazeProjectStructure structure = EazeProjectStructure.forProject(literal.getProject());
        if (structure != null && structure.isValidEazeUri(literal.getContents())) {
            return new PsiReference[] { new EazeUriReference(literal, literal.getValueRange()) };
        }
        return PsiReference.EMPTY_ARRAY;
    }
}
