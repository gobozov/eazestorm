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
import javax.swing.tree.TreePath;
import java.awt.*;

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
    private JTextField webDirTextField;
    @NotNull
    private Settings settings;
    private boolean isModified;
    private String value;

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

//        webDirTextField = new JTextField(settings.getStringValue(Settings.KEY_WEB_DIR, "web"));
//
//        webDirTextField.getDocument().addDocumentListener(new DocumentListener() {
//            public void changedUpdate(DocumentEvent e) {
//                isModified = true;
//            }
//
//            public void removeUpdate(DocumentEvent e) {
//                isModified = true;
//            }
//
//            public void insertUpdate(DocumentEvent e) {
//                isModified = true;
//            }
//        });
//
//
//        @NotNull JPanel buttonWrapper = new JPanel(new GridLayout(1, 3));
//        buttonWrapper.setBorder(IdeBorderFactory.createEmptyBorder(3));
//        buttonWrapper.add(webDirTextField);
//        buttonWrapper.add(new Spacer());
//        buttonWrapper.add(new Spacer());
//        cachePanel.add(buttonWrapper, BorderLayout.SOUTH);
//
//        cachePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, cachePanel.getPreferredSize().height));
//        add(cachePanel);

        FileSystemTreeFactory treeFactory = new FileSystemTreeFactoryImpl();
        FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
        descriptor.setIsTreeRootVisible(true);
        descriptor.setRoots(settings.getProject().getBaseDir());
        final FileSystemTree tree = treeFactory.createFileSystemTree(settings.getProject(), descriptor);
        final JTree jTree = tree.getTree();

        jTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {

                if (e.getPath() != null){
                    String webDirPath = e.getPath().getLastPathComponent().toString();
                    LOGGER.info("WebDir lasp path = " + webDirPath);
                    LOGGER.info("tree path = " + tree.getSelectedFile().getName());
                    value = webDirPath;
                    isModified = true;
                }
            }
        });

        String webDirPath = settings.getStringValue(Settings.KEY_WEB_DIR, "web");
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

    @NotNull
    public JTextField getWebDirTextField() {
        return webDirTextField;
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
}
