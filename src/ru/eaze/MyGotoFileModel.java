package ru.eaze; /**
 * Created by IntelliJ IDEA.
 * User: zenden
 * Date: 01.12.11
 * Time: 11:36
 * To change this template use File | Settings | File Templates.
 */


import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.ide.util.gotoByName.GotoFileCellRenderer;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.eaze.domain.EazeProjectStructure;

import java.util.ArrayList;
import java.util.List;

public class MyGotoFileModel implements MyModel {
    private final int myMaxSize;

    EazeProjectStructure eazeProjectStructure = null;
    List<String> cachedFileList = new ArrayList<String>();
    String cachedPattern = null;
    Project myProject = null;


    public MyGotoFileModel(Project project, VirtualFile webDir) {
        myProject = project;
        eazeProjectStructure = EazeProjectStructure.forProject(project);
        myMaxSize = WindowManagerEx.getInstanceEx().getFrame(project).getSize().width;
    }

    protected boolean acceptItem(final NavigationItem item) {
        return true;
    }

    @Nullable
    //@Override
    protected FileType filterValueFor(NavigationItem item) {
        return item instanceof PsiFile ? ((PsiFile) item).getFileType() : null;
    }

    public String getPromptText() {
        return "Enter page URL (or Eaze URI):";
    }

    public String getCheckBoxName() {
        return IdeBundle.message("checkbox.include.non.project.files");
    }

    public char getCheckBoxMnemonic() {
        return SystemInfo.isMac ? 'P' : 'n';
    }

    public String getNotInMessage() {
        return IdeBundle.message("label.no.non.java.files.found");
    }

    public String getNotFoundMessage() {
        return IdeBundle.message("label.no.files.found");
    }

    public boolean loadInitialCheckBoxState() {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
        return propertiesComponent.isTrueValue("GoToClass.includeJavaFiles");
    }

    public void saveInitialCheckBoxState(boolean state) {
        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(myProject);
        propertiesComponent.setValue("GoToClass.includeJavaFiles", Boolean.toString(state));
    }

    public PsiElementListCellRenderer getListCellRenderer() {
        //return new DefaultPsiElementCellRenderer();
        return new GotoFileCellRenderer(myMaxSize);
    }

    @Nullable
    public String getFullName(final Object element) {
        if (element instanceof PsiFile) {
            final VirtualFile virtualFile = ((PsiFile) element).getVirtualFile();
            return virtualFile != null ? virtualFile.getPath() : null;
        }

        return getElementName(element);
    }

    @NotNull
    public String[] getSeparators() {
        return new String[]{"/", "\\"};
    }

    public String getHelpId() {
        return "procedures.navigating.goto.class";
    }

    public boolean willOpenEditor() {
        return true;
    }

    public Object[] getElementsByName(String name, boolean checkBoxState, String pattern) {
        return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getElementName(Object element) {
        return "ololo";
    }

    public String[] getNames(boolean checkBoxState) {
        return new String[]{"ha", "bred"};
    }

    public Object[] getElementsByPattern(String userPattern) {
        Object[] res = eazeProjectStructure.getFileNamesForURL(userPattern, cachedFileList);
        return res;

    }
}