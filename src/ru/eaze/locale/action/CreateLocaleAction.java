package ru.eaze.locale.action;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import icons.EazeStormIcons;
import org.jetbrains.annotations.NotNull;
import ru.eaze.locale.EazeLocaleUtil;

import javax.swing.*;

public class CreateLocaleAction extends BaseIntentionAction implements Iconable {

    private static final String ERROR_TITLE = "Create Localization Error";
    private static final String INVALID_KEY = "Invalid localization key";
    private static final String NOT_XML_FILE = "Not an XML file";
    private static final String NOT_FOUND_FILE = "Document not found";
    private static final String INVALID_FILE = "Not a localization file. File was deleted or has invalid root tag.";

    private final VirtualFile localeFile;
    private final String localeKey;

    public CreateLocaleAction(String localeKey, VirtualFile localeFile) {
        this.localeKey = localeKey;
        this.localeFile = localeFile;
    }

    /**
     * Returns the name of the family of intentions. It is used to externalize
     * "auto-show" state of intentions. When user clicks on a lightbulb in intention list,
     * all intentions with the same family name get enabled/disabled.
     * The name is also shown in settings tree.
     *
     * @return the intention family name.
     * @see com.intellij.codeInsight.intention.IntentionManager#registerIntentionAndMetaData(com.intellij.codeInsight.intention.IntentionAction, String...)
     */
    @NotNull
    @Override
    public String getFamilyName() {
        return "EazeStorm";
    }

    /**
     * Checks whether this intention is available at a caret offset in file.
     * If this method returns true, a light bulb for this intention is shown.
     *
     * @param project the project in which the availability is checked.
     * @param editor  the editor in which the intention will be invoked.
     * @param file    the file open in the editor.
     * @return true if the intention is available, false otherwise.
     */
    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return true;
    }

    /**
     * Returns text to be shown in the list of available actions, if this action
     * is available.
     *
     * @see #isAvailable(Project,Editor,PsiFile)
     * @return the text to show in the intention popup.
     */
    @NotNull
    @Override
    public String getText() {
        String text = "Create ";
        if (localeKey.endsWith(EazeLocaleUtil.LOCALE_KEY_DELIMITER)) {
            text += "path \"" + localeKey.substring(0, localeKey.length() - 1) + "\" ";
        } else {
            text += "localization ";
        }
        if (localeFile != null && localeFile.isValid()) {
            text += "in [" + localeFile.getName() + "]";
        } else {
            text += "in new file";
        }
        return text;
    }

    @Override
    public Icon getIcon(@IconFlags int flags) {
        return EazeStormIcons.QuickFix;
    }

    /**
     * Called when user invokes intention. This method is called inside command.
     * If {@link #startInWriteAction()} returns true, this method is also called
     * inside write action.
     *
     * @param project the project in which the intention is invoked.
     * @param editor  the editor in which the intention is invoked.
     * @param file    the file open in the editor.
     */
    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        String text = null;
        if (!localeKey.endsWith(EazeLocaleUtil.LOCALE_KEY_DELIMITER)) {
            text = Messages.showInputDialog(project, "Enter text for key " + localeKey, CreateLocaleAction.this.getText(), null, null, new InputValidator() {
                @Override
                public boolean checkInput(String inputString) {
                    return inputString != null && !inputString.isEmpty();
                }

                @Override
                public boolean canClose(String inputString) {
                    return inputString != null && !inputString.isEmpty();
                }
            });
            if (text == null || text.isEmpty()) {
                return; //canceled
            }
        }
        createLocalization(project, localeFile, localeKey, text, false);
    }

    public static boolean createLocalization(final Project project, final VirtualFile file, final String key, final String text, final boolean silentMode) {
        if (!EazeLocaleUtil.isValidKey(key)) {
            if (!silentMode) Messages.showErrorDialog(project, INVALID_KEY, ERROR_TITLE);
            return false;
        }
        if (file == null || !file.isValid()) {
            if (!silentMode) Messages.showErrorDialog(project, INVALID_FILE, ERROR_TITLE); //TODO file creation
            return false;
        }

        final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (!(psiFile instanceof XmlFile)) {
            if (!silentMode) Messages.showErrorDialog(project, NOT_XML_FILE, ERROR_TITLE);
            return false;
        }
        final PsiDocumentManager manager = PsiDocumentManager.getInstance(psiFile.getProject());
        final Document document = manager.getDocument(psiFile);
        if (document == null) {
            if (!silentMode) Messages.showErrorDialog(project, NOT_FOUND_FILE, ERROR_TITLE);
            return false;
        }
        manager.doPostponedOperationsAndUnblockDocument(document);

        return new WriteCommandAction<Boolean>(project) {
            @Override
            protected void run(@NotNull Result<Boolean> result) throws Throwable {
                manager.commitDocument(document);

                XmlTag root = ((XmlFile)psiFile).getRootTag();
                if (root == null || !root.isValid() || !root.getName().equals(EazeLocaleUtil.LOCAL_FILE_ROOT_TAG_NAME)) {
                    if (!silentMode) Messages.showErrorDialog(project, INVALID_FILE, ERROR_TITLE); //TODO root creation
                    result.setResult(false);
                    return;
                }

                String[] keyParts = EazeLocaleUtil.getKeyParts(key);
                XmlTag tag = root;
                final StringBuilder processedKey = new StringBuilder();
                for (String keyPart : keyParts) {
                    if (processedKey.length() > 0) {
                        processedKey.append(EazeLocaleUtil.LOCALE_KEY_DELIMITER);
                    }
                    processedKey.append(keyPart);
                    XmlTag subTag = tag.findFirstSubTag(keyPart);
                    //second condition allows text replacement
                    if (EazeLocaleUtil.isValueTag(subTag) && processedKey.length() < key.length() - EazeLocaleUtil.LOCALE_KEY_DELIMITER.length()) {
                        String message = "File [" + file.getName() + "] contains value for key [" + processedKey.toString() + "].\n You should fix your localization files manually.";
                        if (!silentMode) Messages.showErrorDialog(project, message, ERROR_TITLE);
                        result.setResult(false);
                        return;
                    }
                    if (subTag == null || !subTag.isValid()) {
                        subTag = tag.createChildTag(keyPart, tag.getNamespace(), "", false);
                        subTag = tag.addSubTag(subTag, false);
                    }
                    tag = subTag;
                }

                if (text != null) {
                    tag.getValue().setText(text);
                }
                manager.doPostponedOperationsAndUnblockDocument(document);
                manager.commitDocument(document);

                result.setResult(true);
            }
        }.execute().getResultObject();
    }
}
