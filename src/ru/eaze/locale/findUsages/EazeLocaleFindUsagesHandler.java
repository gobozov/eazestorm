package ru.eaze.locale.findUsages;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageInfoFactory;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import ru.eaze.domain.EazeProjectStructure;
import ru.eaze.locale.EazeLocaleDeclaration;
import ru.eaze.locale.EazeLocaleDeclarationSearcher;
import ru.eaze.locale.reference.EazeLocaleNavigationElement;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class EazeLocaleFindUsagesHandler extends FindUsagesHandler {

    protected EazeLocaleFindUsagesHandler(PsiElement psiElement) {
        super(psiElement);
    }

    @Override
    public boolean processElementUsages(@NotNull PsiElement element, @NotNull final Processor<UsageInfo> processor, @NotNull final FindUsagesOptions options) {
        if (element instanceof EazeLocaleDeclaration || element instanceof EazeLocaleNavigationElement) {
            final EazeLocaleDeclaration target = element instanceof EazeLocaleDeclaration ?
                                                (EazeLocaleDeclaration)element :
                                                ((EazeLocaleNavigationElement) element).getDeclaration();
            final String searchString = ApplicationManager.getApplication().runReadAction(new Computable<String>() {
                @Override
                public String compute() {
                    return target.getValue();
                }
            });

            if (searchString.isEmpty()) {
                return false;
            }

            final Project project = ApplicationManager.getApplication().runReadAction(new Computable<Project>() {
                @Override
                public Project compute() {
                    return target.getProject();
                }
            });
            final EazeProjectStructure structure = EazeProjectStructure.forProject(project);
            if (structure == null) {
                return false;
            }

            final UsageInfoFactory factory = new UsageInfoFactory() {
                @Override
                public UsageInfo createUsageInfo(@NotNull PsiElement usage, int startOffset, int endOffset) {
                    if (target.getTextRange() != null
                            && usage.getContainingFile() == target.getContainingFile()
                            && target.getTextRange().contains(startOffset)
                            && target.getTextRange().contains(endOffset)) {
                        return null;
                    }

                    return new UsageInfo(usage, startOffset, endOffset, true);
                }
            };

            final Map<VirtualFile, Collection<String>> keyFiles = new HashMap<VirtualFile, Collection<String>>();
            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    Collection<String> keys = FileBasedIndex.getInstance().getAllKeys(EazeLocaleUsagesIndex.NAME, project);
                    for (String key : keys) {
                        if (key.startsWith(searchString)) {
                            Collection<VirtualFile> files = FileBasedIndex.getInstance().getContainingFiles(EazeLocaleUsagesIndex.NAME, key, structure.projectScope());
                            for (VirtualFile file : files) {
                                Collection<String> fileKeys = keyFiles.get(file);
                                if (fileKeys == null) {
                                    fileKeys = new TreeSet<String>();
                                    keyFiles.put(file, fileKeys);
                                }
                                fileKeys.add(key);
                            }
                        }
                    }
                }
            });

            FileBasedIndex.ValueProcessor<Collection<Integer>> offsetProcessor = new FileBasedIndex.ValueProcessor<Collection<Integer>>() {
                @Override
                public boolean process(final VirtualFile file, final Collection<Integer> offsets) {
                    return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
                        @Override
                        public Boolean compute() {
                            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                            if (psiFile != null) {
                                for (Integer offset : offsets) {
                                    PsiElement el = PsiUtilCore.getElementAtOffset(psiFile, offset);
                                    EazeLocaleDeclaration declaration = EazeLocaleDeclarationSearcher.findDeclaration(el, false);
                                    if (declaration != null) {
                                        int start = declaration.getValueRange().getStartOffset();
                                        int end = declaration.getValueRange().getStartOffset() + searchString.length();
                                        UsageInfo info = factory.createUsageInfo(declaration, start, end);
                                        if (!processor.process(info)) {
                                            return false;
                                        }
                                    }
                                }
                            }
                            return true;
                        }
                    });
                }
            };

            for (VirtualFile file : keyFiles.keySet()) {
                for (String key : keyFiles.get(file)) {
                    if (!FileBasedIndex.getInstance().processValues(EazeLocaleUsagesIndex.NAME, key, file, offsetProcessor, structure.projectScope())) {
                        return false;
                    }
                }
            }

            return true;
        }

        return super.processElementUsages(element, processor, options);
    }

    @Override
    protected boolean isSearchForTextOccurencesAvailable(@NotNull PsiElement psiElement, boolean isSingleFile) {
        return true;
    }
}
