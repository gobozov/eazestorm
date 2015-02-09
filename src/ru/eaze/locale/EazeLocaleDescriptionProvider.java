package ru.eaze.locale;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementDescriptionLocation;
import com.intellij.psi.ElementDescriptionProvider;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageViewShortNameLocation;
import com.intellij.usageView.UsageViewTypeLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.eaze.locale.reference.EazeLocaleNavigationElement;

public class EazeLocaleDescriptionProvider implements ElementDescriptionProvider {

    private static final String EAZE_LOCALE_KEY_TYPE = "Eaze Localization Key";

    @Nullable
    @Override
    public String getElementDescription(@NotNull PsiElement element, @NotNull ElementDescriptionLocation location) {
        if (element instanceof EazeLocaleNavigationElement) {
            EazeLocaleNavigationElement navElement = (EazeLocaleNavigationElement)element;
            if (location instanceof UsageViewShortNameLocation) {
                return StringUtil.escapeXml(EazeLocaleUtil.extractTagValue(navElement.getTag()));
            }
            if (location instanceof UsageViewTypeLocation) {
                return "";
            }
        }
        if (element instanceof EazeLocaleDeclaration) {
            EazeLocaleDeclaration declaration = (EazeLocaleDeclaration) element;
            if (location instanceof UsageViewShortNameLocation) {
                return declaration.getValue();
            }
            if (location instanceof UsageViewTypeLocation) {
                return EAZE_LOCALE_KEY_TYPE;
            }
        }
        return null;
    }
}
