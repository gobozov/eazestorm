package ru.eaze.component;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;

/**
 * Created by IntelliJ IDEA.
 * User: user
 * Date: 28.01.12
 * Time: 16:22
 * To change this template use File | Settings | File Templates.
 */

public class MyDocumentManagerListener implements PsiDocumentManager.Listener {

    public void fileCreated(com.intellij.psi.PsiFile psiFile, com.intellij.openapi.editor.Document document) {
        if (psiFile != null && psiFile.getName().endsWith(".xml")) {
            VirtualFile file = psiFile.getVirtualFile();
            VirtualFile containingDirectory = file.getParent();

            String path = containingDirectory.getParent().getPath();
            if (path.endsWith("/web/lib")) {
                document.addDocumentListener(new MyPackageXmlListener(psiFile));
            }

        }
    }

    public void documentCreated(com.intellij.openapi.editor.Document document, com.intellij.psi.PsiFile psiFile) {

    }


}
