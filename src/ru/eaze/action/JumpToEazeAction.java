package ru.eaze.action;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import ru.eaze.MyGotoFileModel;
import ru.eaze.settings.Settings;
import ru.eaze.domain.MyListElement;
import ru.eaze.MyModel;
import ru.eaze.MyPopup;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: zenden
 * Date: 01.12.11
 * Time: 10:36
 */
public class JumpToEazeAction extends AnAction implements DumbAware, MyModel.Callback {

    private Project project;
    private Settings settings;
    protected static Class myInAction = null;
    private static Map<Class, Pair<String, Integer>> ourLastStrings = new HashMap<Class, Pair<String, Integer>>();

    public JumpToEazeAction() {

    }

    public void actionPerformed(final AnActionEvent e) {
        myInAction = JumpToEazeAction.class;
        project = e.getData(PlatformDataKeys.PROJECT);
        final Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);

        settings = new Settings(project);

        VirtualFile baseDir = project.getBaseDir();
        VirtualFile webDir = baseDir.findFileByRelativePath(settings.getStringValue(Settings.KEY_WEB_DIR, "web"));

        if (webDir == null) {
            Messages.showErrorDialog("Could not find 'web' directory! it should be in root folder of your project! Check web dir in settings.", "EazeStorm");
            return;
        }
        showNavigationPopup(e, new MyGotoFileModel(project, webDir), this, "bred");
    }

    protected <T> void showNavigationPopup(AnActionEvent e, MyModel model, MyModel.Callback callback/*, final GotoActionCallback<T> callback*/) {
        showNavigationPopup(e, model, callback, null);
    }

    protected <T> void showNavigationPopup(AnActionEvent e, MyModel model, MyModel.Callback callback
                                           /*final GotoActionCallback<T> callback*/, final String findTitle) {
        final Project project = e.getData(PlatformDataKeys.PROJECT);

        boolean mayRequestOpenInCurrentWindow = model.willOpenEditor() && FileEditorManagerEx.getInstanceEx(project).hasSplitOrUndockedWindows();
        final Class startedAction = /*myInAction*/JumpToEazeAction.class;

        Pair<String, Integer> start = getInitialText(e.getData(PlatformDataKeys.EDITOR));
        final MyPopup popup = MyPopup.createPopup(project, model, /*getPsiContext(e),*/ start.first, mayRequestOpenInCurrentWindow, start.second);
        popup.invoke(this, ModalityState.current(), true);
    }


    private static Pair<String, Integer> getInitialText(Editor editor) {
        if (editor != null) {

        }
        return Pair.create("", 0);
    }

    /*@Override   */
    public void elementChosen(final Object element) {
        if (element == null) return;
        ApplicationManager.getApplication().invokeLater(
                new Runnable() {
                    public void run() {
                        MyListElement el = (MyListElement) element;
                        VirtualFile file = el.getFile();

                        if (file != null) {
                            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

                            if (psiFile.canNavigate()) {
                                psiFile.navigate(true);
                            }
                        }
                    }
                }

        );

    }

    public void onClose(final MyPopup popup) {
        ourLastStrings.put(myInAction, Pair.create(popup.getEnteredText(), popup.getSelectedIndex()));
    }

}
