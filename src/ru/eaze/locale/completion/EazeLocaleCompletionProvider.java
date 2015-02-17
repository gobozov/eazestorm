package ru.eaze.locale.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ProcessingContext;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import ru.eaze.domain.EazeProjectStructure;
import ru.eaze.locale.EazeLocaleDeclaration;
import ru.eaze.locale.EazeLocaleDeclarationSearcher;
import ru.eaze.locale.EazeLocaleKeyIndex;
import ru.eaze.locale.EazeLocaleUtil;

import java.util.Collection;

public class EazeLocaleCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {
        if (!EazeLocaleUtil.inScope(parameters.getOriginalPosition())) {
            return;
        }
        EazeLocaleDeclaration declaration = EazeLocaleDeclarationSearcher.findDeclaration(parameters.getOriginalPosition());
        EazeProjectStructure structure = EazeProjectStructure.forProject(parameters.getOriginalPosition().getProject());
        if (declaration != null && structure != null) {
            final String prefix = declaration.getValue();
            final GlobalSearchScope scope = structure.projectScope();
            final CompletionResultSet resultSet = result.withPrefixMatcher(prefix);
            final CompletionType completionType = parameters.getCompletionType();
            Processor<String> processor = new Processor<String>() {
                @Override
                public boolean process(String key) {
                    if (key.contains(prefix)) {
                        Collection<VirtualFile> files = FileBasedIndex.getInstance().getContainingFiles(EazeLocaleKeyIndex.NAME, key, scope);
                        if (files.size() > 0) {
                            if (completionType == CompletionType.SMART) {
                                if (key.startsWith(prefix)) {
                                    int end = key.indexOf(EazeLocaleUtil.LOCALE_KEY_DELIMITER, prefix.length());
                                    if (end >= 0) {
                                        key = key.substring(0, end);
                                    }
                                }
                            }
                            resultSet.addElement(LookupElementBuilder.create(key));
                        }
                    }
                    return true;
                }
            };
            FileBasedIndex.getInstance().processAllKeys(EazeLocaleKeyIndex.NAME, processor, parameters.getEditor().getProject());
        }
    }
}
