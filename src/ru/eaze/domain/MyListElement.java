package ru.eaze.domain;

import com.intellij.openapi.vfs.VirtualFile;

public class MyListElement {

    private String title;
    private VirtualFile file;
    private String descr;
    private String actionName;
    private boolean subElement = false;

    public MyListElement(String title, VirtualFile file, String descr, String _actionName) {
        this.title = title;
        this.file = file;
        this.descr = descr;
        this.actionName = _actionName;
    }

    public MyListElement(String title, VirtualFile file, String descr, String _actionName, boolean _subElement) {
        this.title = title;
        this.file = file;
        this.descr = descr;
        this.actionName = _actionName;
        this.subElement = _subElement;
    }

    public String getTitle() {
        return title;
    }

    public VirtualFile getFile() {
        return file;
    }

    @Override
    public String toString() {
        if (file == null) {
            return "empty";
        }
        String result = "[" + descr + "]  " + file.getName();
        if (actionName.length() != 0) {
            result += "              (" + actionName + ")";
        }
        if (subElement) {
            result = "   " + result;
        }
        return result;
    }

    boolean getSubElement() {
        return subElement;
    }
}