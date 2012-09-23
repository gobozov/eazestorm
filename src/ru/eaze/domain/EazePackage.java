package ru.eaze.domain;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: gb
 * Date: 23.09.12
 * Time: 20:22
 * To change this template use File | Settings | File Templates.
 */
public class EazePackage {
    String packageName;
    VirtualFile webDir;
    //final Project project;
    HashMap<String, EazeAction> actions = new HashMap<String, EazeAction>();

    public EazePackage(String packageName) {
        this.packageName = packageName;
    }

    public EazePackage(String packageName, final Project project, VirtualFile sourceFile, VirtualFile webDir ) {
        // this.project = project;
        this.packageName = packageName;
        this.webDir = webDir;
        readActionsXml(project, sourceFile);
    }

    void addEazeAction(EazeAction action) {
        actions.put(action.getName(), action);
    }

    public EazeAction getActionByName(String name) {
        return actions.get(name);
    }

    public String[] getAvailableActionNames() {
        ArrayList<String> arrayList = new ArrayList<String>();

        for (String key : actions.keySet()) {
            arrayList.add(packageName + "." + key);

        }

        return arrayList.toArray(new String[0]);
    }

    // actions.xml
    public void analyzePageOrGroupTag(XmlTag parentTag) {
        XmlTag[] pagesOrPagesGroupsTags = parentTag.getSubTags();
        for (XmlTag tag : pagesOrPagesGroupsTags) {
            String tagName = tag.getName();
            if (tagName.equals("action")) {
                String actionName = tag.getAttribute("name").getValue().toString();
                String path = "";
                XmlTag pathTag = tag.findFirstSubTag("path");
                if (pathTag != null) {
                    path = pathTag.getValue().getText();
                }
                if (path == "") {
                    path = actionName;
                }
                path += ".php";

                path = "/lib/" + packageName + "/actions/" + path;
                VirtualFile file = webDir.findFileByRelativePath(path);
                EazeAction action = new EazeAction(actionName, path, file, tag);
                actions.put(actionName, action);

            } else if (tagName.equals("group")) {
                analyzePageOrGroupTag(tag);
            }
        }
    }


    public void readActionsXml(final Project project, VirtualFile file) {
        Map<String, String> map = new HashMap<String, String>();
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile != null) {
            XmlFile xml = (XmlFile) psiFile;
            XmlTag sitesTag = xml.getRootTag();

            this.analyzePageOrGroupTag(sitesTag);

        }
    }
}
