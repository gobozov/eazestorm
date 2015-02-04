package ru.eaze.locale.action;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import ru.eaze.locale.EazeLocaleUtil;

import javax.swing.*;

public class CreateLocaleIntentionAction extends BaseIntentionAction implements Iconable {

    private static final String ERROR_TITLE = "Create Localization Error";
    private static final String INVALID_KEY = "Invalid localization key";
    private static final String NOT_XML_FILE = "Not an XML file";
    private static final String NOT_FOUND_FILE = "Document not found";
    private static final String INVALID_FILE = "Not a localization file";

    private final VirtualFile localeFile;
    private final String localeKey;

    public CreateLocaleIntentionAction(String localeKey, VirtualFile localeFile) {
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
        String text = "Create localization in ";
        if (localeFile != null && localeFile.isValid()) {
            text += localeFile.getName();
        } else {
            text += "new file";
        }
        return text;
    }

    @Override
    public Icon getIcon(@IconFlags int flags) {
        return AllIcons.Actions.QuickfixBulb;
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
    public void invoke(@NotNull final Project project, final Editor editor, PsiFile file) throws IncorrectOperationException {
        if (!EazeLocaleUtil.isValidKey(localeKey)) {
            Messages.showErrorDialog(project, INVALID_KEY, ERROR_TITLE);
            return;
        }

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                if (localeFile == null || !localeFile.isValid()) {
                    return; //TODO file creation
                }

                PsiFile psiFile = PsiManager.getInstance(project).findFile(localeFile);
                if (!(psiFile instanceof XmlFile)) {
                    Messages.showErrorDialog(project, NOT_XML_FILE, ERROR_TITLE);
                    return;
                }
                PsiDocumentManager manager = PsiDocumentManager.getInstance(psiFile.getProject());
                Document document = manager.getDocument(psiFile);
                if (document == null) {
                    Messages.showErrorDialog(project, NOT_FOUND_FILE, ERROR_TITLE);
                    return;
                }
                manager.commitDocument(document);

                XmlTag root = ((XmlFile)psiFile).getRootTag();
                if (root == null || !root.isValid() || !root.getName().equals(EazeLocaleUtil.LOCAL_FILE_ROOT_TAG_NAME)) {
                    Messages.showErrorDialog(project, INVALID_FILE, ERROR_TITLE);
                    return;
                }

                String text = Messages.showInputDialog(project, "Enter text for key " + localeKey, CreateLocaleIntentionAction.this.getText(), null, null, new InputValidator() {
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

                String[] keyParts = EazeLocaleUtil.getKeyParts(localeKey);
                XmlTag tag = root;
                StringBuilder processedKey = new StringBuilder();
                for (String keyPart : keyParts) {
                    if (processedKey.length() > 0) {
                        processedKey.append(EazeLocaleUtil.LOCALE_KEY_DELIMITER);
                    }
                    processedKey.append(keyPart);
                    XmlTag subTag = tag.findFirstSubTag(keyPart);
                    if (EazeLocaleUtil.isValueTag(subTag)) {
                        String message = "File [" + localeFile.getName() + "] contains value for key [" + processedKey.toString() + "].\n You should fix your localization files manually.";
                        Messages.showErrorDialog(project, message, ERROR_TITLE);
                        return;
                    }
                    if (subTag == null || !subTag.isValid()) {
                        subTag = tag.createChildTag(keyPart, tag.getNamespace(), "", true);
                        tag.addSubTag(subTag, false);
                    }
                    tag = subTag;
                }

                manager.doPostponedOperationsAndUnblockDocument(document);
                manager.commitDocument(document);

                //first document commit doesn't save new tag text applied with tag.getValue().setText(text)
                tag = manager.commitAndRunReadAction(new Computable<XmlTag>() {
                    @Override
                    public XmlTag compute() {
                        return EazeLocaleUtil.findTagForKey(project, localeFile, localeKey);
                    }
                });
                tag.getValue().setText(text);
                manager.doPostponedOperationsAndUnblockDocument(document);
                manager.commitDocument(document);

                manager.commitAndRunReadAction(new Runnable() {
                    @Override
                    public void run() {
                        XmlTag tag = EazeLocaleUtil.findTagForKey(project, localeFile, localeKey);
                        assert tag != null;
                        assert EazeLocaleUtil.isValueTag(tag);
                    }
                });
            }
        });
    }
}
