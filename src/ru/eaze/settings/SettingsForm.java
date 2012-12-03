package ru.eaze.settings;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileSystemTree;
import com.intellij.openapi.fileChooser.FileSystemTreeFactory;
import com.intellij.openapi.fileChooser.ex.FileSystemTreeFactoryImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.uiDesigner.core.Spacer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.Enumeration;

/**
 * Created with IntelliJ IDEA.
 * User: GGobozov
 * Date: 29.11.12
 * Time: 15:21
 * To change this template use File | Settings | File Templates.
 */
public class SettingsForm extends JPanel {

    private Logger LOGGER = Logger.getInstance(SettingsForm.class);

    @NotNull
    private Settings settings;
    private boolean isModified;
    private String pathValue;

    public SettingsForm(Settings settings) {
        this.settings = settings;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        @NotNull final JPanel treePanel = new JPanel(new BorderLayout());
        @NotNull final Border border = new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(3, 5, 3, 5));
        treePanel.setBorder(new TitledBorder(border, "EazeStorm Settings"));

        @NotNull final JPanel gridWrapper = new JPanel(new GridLayout(1, 2));
        gridWrapper.add(new JLabel("Web directory name"));
        gridWrapper.add(new JLabel());
        treePanel.add(gridWrapper, BorderLayout.NORTH);


        FileSystemTreeFactory treeFactory = new FileSystemTreeFactoryImpl();
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        descriptor.setIsTreeRootVisible(true);
        descriptor.setRoots(settings.getProject().getBaseDir());
        final FileSystemTree tree = treeFactory.createFileSystemTree(settings.getProject(), descriptor);
        final JTree jTree = tree.getTree();


        jTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {

                if (e.getPath() != null) {
                    //TreePath path = (TreePath) e.getPath().getLastPathComponent();
                    pathValue = e.getPath().getLastPathComponent().toString();
                    isModified = true;
                }
            }
        });

        String webDirPath = settings.getStringValue(Settings.KEY_WEB_DIR, "web");
        VirtualFile vf = findDir(settings.getProject().getBaseDir(), webDirPath);
        if (vf != null)
            tree.select(vf, null);
//        TreeModel treeModel = jTree.getModel();
//        TreePath treePath = find((DefaultMutableTreeNode) jTree.getModel().getRoot(), webDirPath);
//        if (treePath != null) {
//            jTree.setSelectionPath(treePath);
//            jTree.scrollPathToVisible(treePath);
//        }


        walk(jTree.getModel(), jTree.getModel().getRoot());

        //TreePath treePath = new TreePath(webDirPath);
        //jTree.expandPath(treePath);
        //jTree.getSelectionModel().setSelectionPath(null);

        // VirtualFile webDir = settings.getProject().getBaseDir().findFileByRelativePath(webDirPath);
        // tree.select(webDir, null);

//        if (settings.getStringValue(Settings.KEY_WEB_DIR, "web") != null){
//            TreePath webDirPath = new TreePath(value);
//            jTree.expandPath(webDirPath);
//            jTree.setSelectionPath(webDirPath);
//        }


        treePanel.add(jTree, BorderLayout.CENTER);
        add(treePanel);


    }

    protected VirtualFile findDir(VirtualFile root, String name){
        for (VirtualFile vf: root.getChildren()){
            if (vf.isDirectory() && vf.getName().equals(name)) return vf;
            if (vf.isDirectory()){
                VirtualFile file = findDir(vf, name);
                if (file != null)
                    return file;
            }
        }
        return null;
    }

    protected void walk(TreeModel model, Object o) {
        int count = model.getChildCount(o);
        for (int i = 0; i < count; i++) {
            Object child = model.getChild(o, i);
            if (model.isLeaf(child))
                System.out.println(child.toString());
            else
                walk(model, child);
        }
    }

    private TreePath find(DefaultMutableTreeNode root, String s) {
        if (root == null)
            return null;
        @SuppressWarnings("unchecked")
        Enumeration<DefaultMutableTreeNode> e = root.depthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = e.nextElement();
            if (node == null) continue;
            if (node.toString().equalsIgnoreCase(s)) {
                return new TreePath(node.getPath());
            }
        }
        return null;
    }


    public boolean isModified() {
        return isModified;
    }

    @NotNull
    public Settings getSettings() {
        return settings;
    }

    public void setSettings(@NotNull Settings settings) {
        this.settings = settings;
    }

    public String getPathValue() {
        return pathValue;
    }

    public void setPathValue(String pathValue) {
        this.pathValue = pathValue;
    }
}
