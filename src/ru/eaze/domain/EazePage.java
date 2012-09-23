package ru.eaze.domain;

import com.intellij.openapi.vfs.VirtualFile;
import org.omg.PortableInterceptor.ServerRequestInfo;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: user
 * Date: 01.12.11
 * Time: 23:50
 */
public class EazePage {

    private String templatePath;
    private String[] actions;
    private String[] bootActions;
    private String[] shutdownActions;
    private String translatedURI;
    private VirtualFile file;

    public EazePage( VirtualFile file, String templatePath, String translatedURI, String[] actions, String[] _bootActions, String[] _shutdownActions ){
        this.templatePath = templatePath;
        this.translatedURI = translatedURI;
        this.actions = actions;
        this.file = file;
        this.bootActions = _bootActions;
        this.shutdownActions = _shutdownActions;
    }

    String getTemplatePath(){
        return templatePath;
    }

    String getTranslatedURI() {
        return translatedURI;
    }

    String[] getActions() {
        return actions;
    }

    VirtualFile getFile() {
        return file;
    }

    String[] getBootActions() {
       return bootActions;
    }

    String[] getShutdownActions() {
        return shutdownActions;
    }

}
