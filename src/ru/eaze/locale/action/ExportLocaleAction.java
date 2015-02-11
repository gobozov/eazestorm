package ru.eaze.locale.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.psi.xml.XmlTokenType;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.PhpFile;
import org.jetbrains.annotations.NotNull;
import ru.eaze.locale.EazeLocaleUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ExportLocaleAction extends DumbAwareAction {

    private static Map<String, String> prefixCache = new HashMap<String, String>();

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            setStatus(event, false);
            return;
        }

        PsiFile file = event.getData(CommonDataKeys.PSI_FILE);
        if (!(file instanceof PhpFile)) {
            setStatus(event, false);
            return;
        }

        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if(editor == null) {
            setStatus(event, false);
            return;
        }

        if (EazeLocaleUtil.getLocaleFiles(project).isEmpty()) {
            setStatus(event, false);
            return;
        }

        PsiElement element;
        if(editor.getSelectionModel().hasSelection()) {
            element = file.findElementAt(editor.getSelectionModel().getSelectionStart());
        } else {
            element = file.findElementAt(editor.getCaretModel().getOffset());
        }
        if(element == null) {
            setStatus(event, false);
            return;
        }

        IElementType type = element.getNode().getElementType();
        if(type == XmlTokenType.XML_DATA_CHARACTERS || type == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN
                || type == PhpTokenTypes.STRING_LITERAL || type == PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE) {
            setStatus(event, true);
        } else {
            setStatus(event, false);
        }
    }

    private void setStatus(AnActionEvent event, boolean status) {
        event.getPresentation().setVisible(status);
        event.getPresentation().setEnabled(status);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final Project project = event.getProject();
        if (project == null) {
            return;
        }

        PsiFile file = event.getData(CommonDataKeys.PSI_FILE);
        if (!(file instanceof PhpFile)) {
            return;
        }

        final Editor editor = event.getData(CommonDataKeys.EDITOR);
        if(editor == null) {
            return;
        }

        String text = editor.getSelectionModel().getSelectedText();
        int startOffset;
        int endOffset;
        if(text != null) {
            startOffset = editor.getSelectionModel().getSelectionStart();
            endOffset = editor.getSelectionModel().getSelectionEnd();
        } else {
            PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
            if(element == null) {
                return;
            }

            IElementType type = element.getNode().getElementType();
            if(!(type == XmlTokenType.XML_DATA_CHARACTERS || type == XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN
                    || type == PhpTokenTypes.STRING_LITERAL || type == PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE)) {
                return;
            }

            startOffset = element.getTextRange().getStartOffset();
            endOffset = element.getTextRange().getEndOffset();
            text = element.getText();
        }

        String keyPrefix = "";
        if (file.getVirtualFile() != null) {
            keyPrefix = prefixCache.get(file.getVirtualFile().getPath());
        }
        ExportLocaleDialog dialog = new ExportLocaleDialog(project, text, keyPrefix, new Callback(project, editor, startOffset, endOffset));
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private static class Callback implements ExportLocaleDialog.Callback {
        private final Project project;
        private final Editor editor;
        private final int startOffset;
        private final int endOffset;

        public Callback(@NotNull Project project, @NotNull Editor editor, int startOffset, int endOffset) {
            this.project = project;
            this.editor = editor;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }

        @Override
        public void onOK(Collection<VirtualFile> files, final String key, final String text) {
            int prefixEnd = key.lastIndexOf(EazeLocaleUtil.LOCALE_KEY_DELIMITER);
            PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
            if (psiFile != null && psiFile.getVirtualFile() != null && prefixEnd > 0) {
                prefixCache.put(psiFile.getVirtualFile().getPath(), key.substring(0, prefixEnd + 1));
            }

            boolean created = false;
            for (final VirtualFile file : files) {
                created |= CreateLocaleAction.createLocalization(project, file, key, text, false);
            }
            if (created) {
                PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

                new WriteCommandAction(project) {
                    @Override
                    protected void run(@NotNull Result result) throws Throwable {
                        String insertString = String.format("{lang:%s}", key);
                        editor.getDocument().replaceString(startOffset, endOffset, insertString);
                        editor.getCaretModel().moveToOffset(startOffset + insertString.length());
                    }
                }.execute();
            }
        }
    }
}
