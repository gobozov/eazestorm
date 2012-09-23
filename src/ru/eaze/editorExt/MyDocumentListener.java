package ru.eaze.editorExt;

import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.psi.PsiFile;
import com.intellij.openapi.vfs.VirtualFile;
/**
 * Created by IntelliJ IDEA.
 * User: user
 * Date: 28.01.12
 * Time: 16:26
 * To change this template use File | Settings | File Templates.
 */
public class MyDocumentListener implements DocumentListener {
    private PsiFile psiFile;

    public MyDocumentListener( PsiFile file ) {
        this.psiFile = file;
    }

    public void documentChanged(com.intellij.openapi.editor.event.DocumentEvent documentEvent) {
        VirtualFile  file = psiFile.getVirtualFile();
        VirtualFile containingDirectory = file.getParent();

        String path = containingDirectory.getParent().getPath();
        if ( path.endsWith("/web/lib") ){
           System.out.println(path);
        }
    }

    public void beforeDocumentChange(DocumentEvent documentEvent) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
