package ru.eaze.component;

import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.psi.PsiFile;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;

/**
 * Created by IntelliJ IDEA.
 * User: user
 * Date: 28.01.12
 * Time: 16:26
 * To change this template use File | Settings | File Templates.
 */
public class MyPackageXmlListener implements DocumentListener {
    private PsiFile psiFile;
    
    public MyPackageXmlListener( PsiFile file ) {
        this.psiFile = file;
    }
    
    public void documentChanged(com.intellij.openapi.editor.event.DocumentEvent documentEvent) {
        PsiFile file = psiFile.getParent().getParent().findFile("etc/conf/pages.xml");
        if ( file != null ){
            //PsiFile pagesXmlPsiFile = PsiManager.getInstance(psiFile.getProject()).findFile(file);
           // pagesXmlPsiFile.
        }
    }

    public void beforeDocumentChange(DocumentEvent documentEvent) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
