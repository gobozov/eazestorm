package ru.eaze.domain;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EazeAction {

    private final String name;
    private final XmlTag element;
    private final VirtualFile file;

    public EazeAction(@NotNull String name, @NotNull XmlTag element, VirtualFile file) {
        this.name = name;
        this.file = file;
        this.element = element;
    }

    @NotNull
    public  String getName() {
        return name;
    }

    @Nullable
    public VirtualFile getFile() {
        return file;
    }

    @NotNull
    public PsiElement getNavigationElement() {
        XmlAttribute name = element.getAttribute("name");
        if (name != null && name.getValueElement() != null) {
            return name.getValueElement();
        }
        return element;
    }
}

