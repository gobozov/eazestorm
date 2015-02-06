package ru.eaze.locale.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import com.intellij.util.indexing.FileBasedIndex;
import ru.eaze.locale.EazeLocaleDeclaration;
import ru.eaze.locale.EazeLocaleDeclarationSearcher;
import ru.eaze.locale.EazeLocaleKeyIndex;
import ru.eaze.locale.EazeLocaleUtil;

import java.util.Collection;

public class EazeLocaleCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
        if (!EazeLocaleUtil.inScope(parameters.getOriginalPosition())) {
            return;
        }
        EazeLocaleDeclaration declaration = EazeLocaleDeclarationSearcher.findDeclaration(parameters.getOriginalPosition());
        if (declaration != null) {
            String prefix = declaration.getValue();
            result = result.withPrefixMatcher(prefix);
            Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(EazeLocaleKeyIndex.NAME, parameters.getEditor().getProject());
            for (String key : allKeys) {
                if (parameters.getCompletionType() == CompletionType.SMART) {
                    if (key.startsWith(prefix)) {
                        int end = key.indexOf(EazeLocaleUtil.LOCALE_KEY_DELIMITER, prefix.length());
                        if (end >= 0) {
                            key = key.substring(0, end);
                        }
                    }
                }
                result.addElement(LookupElementBuilder.create(key));
            }
        }
    }
}
