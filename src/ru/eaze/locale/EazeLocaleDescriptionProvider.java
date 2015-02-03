package ru.eaze.locale;

import com.intellij.psi.ElementDescriptionLocation;
import com.intellij.psi.ElementDescriptionProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.usageView.UsageViewShortNameLocation;
import com.intellij.usageView.UsageViewTypeLocation;
import org.jetbrains.annotations.Nullable;
import ru.eaze.locale.reference.EazeLocaleNavigationElement;

public class EazeLocaleDescriptionProvider implements ElementDescriptionProvider {

    private static final String EAZE_LOCALE_KEY_TYPE = "Eaze Localization Key";

    @Nullable
    @Override
    public String getElementDescription(PsiElement element, ElementDescriptionLocation location) {
        if (element instanceof EazeLocaleNavigationElement) {
            EazeLocaleNavigationElement navElement = (EazeLocaleNavigationElement)element;
            if (location instanceof UsageViewShortNameLocation) {
                return EazeLocaleUtil.extractTagValue(navElement.getTag());
            }
            if (location instanceof UsageViewTypeLocation) {
                return "";
            }
        }
        return null;
    }
}
