package ru.eaze.reference;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.Nullable;
import ru.eaze.domain.EazeProjectStructure;
import ru.eaze.reference.MyXmlTagReference;

/**
 * Created by IntelliJ IDEA.
 * User: user
 * Date: 26.01.12
 * Time: 23:07
  Путь
 */
public class EazeUriReference extends MyXmlTagReference {
    String path;
    public  EazeUriReference(String path, PsiElement element, TextRange textRange, EazeProjectStructure structure, Project project){
        super(element, textRange, structure, project);
        this.path = path;
    }

    @Nullable
    public PsiElement resolve() {
        String uri = path;
        if (structure == null) {
            return  null;
        }
        String str = structure.getFirstHost().translateEazePath( uri );
        System.out.println( "translatedPath=" + str );
        VirtualFile webDir = structure.getWebDir();

        VirtualFile targetFile = webDir.findFileByRelativePath( str );
         if ( targetFile != null ){
        return  PsiManager.getInstance(project).findFile( targetFile);
         }

        return null;
    }

   @Override
public String getCanonicalText() {
    return  path;
}

}
