package ru.eaze.locale.refactoring;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.rename.RenameUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.util.CommonProcessors;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.eaze.locale.EazeLocaleDeclaration;
import ru.eaze.locale.findUsages.EazeLocaleFindUsagesHandlerFactory;

import java.util.ArrayList;
import java.util.Collection;

public class EazeLocaleRenameProcessor extends BaseRefactoringProcessor {

    private static final String COMMAND_NAME = "Renaming localization key %s";
    private static final String DECLARATION_HEADER = "Localization key to change";
    private static final String USAGES_HEADER = "Key usages to be changed";
    private static final String REFERENCES_HEADER = "Localization files to be changed";

    private final EazeLocaleDeclaration declaration;
    private final String newName;

    protected EazeLocaleRenameProcessor(@NotNull EazeLocaleDeclaration declaration, @NotNull String newName) {
        super(declaration.getProject());
        this.declaration = declaration;
        this.newName = newName;
    }

    @NotNull
    @Override
    protected UsageViewDescriptor createUsageViewDescriptor(final UsageInfo[] usages) {
        return new UsageViewDescriptor() {
            @NotNull
            @Override
            public PsiElement[] getElements() {
                return new PsiElement[]{ declaration };
            }

            @Override
            public String getProcessedElementsHeader() {
                return DECLARATION_HEADER;
            }

            @Override
            public String getCodeReferencesText(int usagesCount, int filesCount) {
                return USAGES_HEADER;
            }

            @Nullable
            @Override
            public String getCommentReferencesText(int usagesCount, int filesCount) {
                return REFERENCES_HEADER;
            }
        };
    }

    /**
     * Is called inside atomic action.
     */
    @NotNull
    @Override
    protected UsageInfo[] findUsages() {
        Collection<UsageInfo> usages = new ArrayList<UsageInfo>();
        FindUsagesHandler handler = EazeLocaleFindUsagesHandlerFactory.createHandler(declaration);
        handler.processElementUsages(declaration, new CommonProcessors.CollectProcessor<UsageInfo>(usages), new FindUsagesOptions(declaration.getProject()));
        return usages.toArray(new UsageInfo[usages.size()]);
    }

    /**
     * Is called in a command and inside atomic action.
     *
     * @param usages usages selected to rename
     */
    @Override
    protected void performRefactoring(UsageInfo[] usages) {
        try {
            String oldName = declaration.getName();
            RenameUtil.doRename(declaration, newName, UsageInfo.EMPTY_ARRAY, myProject, null);
            for (UsageInfo usage : usages) {
                String usageName = usage.getElement() == null ? null : ((PsiNamedElement)usage.getElement()).getName();
                if (usageName != null) {
                    String usageNewName = usageName.replace(oldName, newName);
                    RenameUtil.doRename(usage.getElement(), usageNewName, UsageInfo.EMPTY_ARRAY, myProject, null);
                }
            }
        } catch (IncorrectOperationException ex) {
            RenameUtil.showErrorMessage(ex, declaration, myProject);
        }

    }

    @Override
    protected String getCommandName() {
        return String.format(COMMAND_NAME, declaration.getValue());
    }
}
