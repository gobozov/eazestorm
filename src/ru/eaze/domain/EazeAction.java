package ru.eaze.domain;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.xml.XmlTag;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: gb
 * Date: 23.09.12
 * Time: 20:21
 * To change this template use File | Settings | File Templates.
 */
public class EazeAction {

    private String name;
    private String path;
    private VirtualFile file;
    private XmlTag element;
    HashMap<String, EazeAction> chainActions = new HashMap<String, EazeAction>();

    public EazeAction(String name, String path, VirtualFile file, XmlTag element) {
        this.name = name;
        this.path = path;
        this.file = file;
        this.element = element;
    }

    public  String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public VirtualFile getFile() {
        return file;
    }

    public XmlTag getXmlTag() {
        return element;
    }
}

