package ru.eaze.domain;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;

public class EazeAction {

    private final String name;
    private final XmlTag element;
    private final VirtualFile file;

    public EazeAction(String name, XmlTag element, VirtualFile file) {
        this.name = name;
        this.file = file;
        this.element = element;
    }

    public  String getName() {
        return name;
    }

    public VirtualFile getFile() {
        return file;
    }

    public PsiElement getNavigationElement() {
        XmlAttribute name = element.getAttribute("name");
        if (name != null && name.getValueElement() != null) {
            return name.getValueElement();
        }
        return element;
    }
}

