package ru.eaze.locale.findUsages;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.eaze.locale.EazeLocaleDeclaration;
import ru.eaze.locale.reference.EazeLocaleNavigationElement;

public class EazeLocaleFindUsagesHandlerFactory extends FindUsagesHandlerFactory {
    @Override
    public boolean canFindUsages(@NotNull PsiElement element) {
        return element.isValid() && (element instanceof EazeLocaleDeclaration || element instanceof EazeLocaleNavigationElement);
    }

    @Nullable
    @Override
    public FindUsagesHandler createFindUsagesHandler(@NotNull PsiElement element, boolean forHighlightUsages) {
        return createHandler(element);
    }

    public  static FindUsagesHandler createHandler(@NotNull PsiElement element) {
        return new EazeLocaleFindUsagesHandler(element);
    }
}
