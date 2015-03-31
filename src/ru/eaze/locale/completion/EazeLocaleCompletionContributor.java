package ru.eaze.locale.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.xml.XmlToken;

public class EazeLocaleCompletionContributor extends CompletionContributor {

    public EazeLocaleCompletionContributor() {
        EazeLocaleCompletionProvider provider = new EazeLocaleCompletionProvider();
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(LeafPsiElement.class), provider);
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(XmlToken.class), provider);
        extend(CompletionType.SMART, PlatformPatterns.psiElement(LeafPsiElement.class), provider);
        extend(CompletionType.SMART, PlatformPatterns.psiElement(XmlToken.class), provider);
    }
}
