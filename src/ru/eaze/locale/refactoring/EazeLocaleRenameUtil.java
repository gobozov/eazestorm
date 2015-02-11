package ru.eaze.locale.refactoring;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.psi.impl.source.xml.XmlTokenImpl;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.util.keyFMap.KeyFMap;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.apache.velocity.runtime.parser.node.ASTNENode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.eaze.locale.EazeLocaleDeclaration;
import ru.eaze.locale.EazeLocaleDeclarationSearcher;
import ru.eaze.locale.EazeLocaleUtil;

public class EazeLocaleRenameUtil {

    private EazeLocaleRenameUtil() { }

    @Nullable
    public static PsiElement renameElement(@NotNull PsiElement element, String oldName, String newName) {
        String text = element.getText().replace(oldName, newName);
        PsiElement newElement = null;

        if (element instanceof StringLiteralExpression) {
            PsiElement literal = PhpPsiElementFactory.createPhpPsiFromText(element.getProject(), element.getNode().getElementType(), text);
            newElement = element.replace(literal);
        }

        if (element instanceof XmlToken) {
            XmlToken token = new XmlTokenImpl(element.getNode().getElementType(), text);
            CodeEditUtil.setNodeGenerated(token.getNode(), true);
            element.getParent().getNode().replaceChild(element.getNode(), token.getNode());
            newElement = token;
        }

        return newElement;
    }

    @Nullable
    public static XmlTag renameKeyTag(@NotNull XmlTag tag, String newName) {
        if (EazeLocaleUtil.canCreateKey(tag.getProject(), newName) && EazeLocaleUtil.isLocaleFile(tag.getContainingFile().getVirtualFile(), tag.getProject())) {
            //create new tag
            String[] names = EazeLocaleUtil.getKeyParts(newName);
            XmlTag root = ((XmlFile)tag.getContainingFile()).getRootTag();
            XmlTag newTag = root;
            for (String name : names) {
                XmlTag subTag = newTag.findFirstSubTag(name);
                if (subTag == null) {
                    subTag = newTag.createChildTag(name, newTag.getNamespace(), "", false);
                    subTag = newTag.addSubTag(subTag, false);
                }
                newTag = subTag;
            }
            //copy children
            if (tag.getSubTags().length == 0) {
                newTag.getValue().setText(tag.getValue().getText());
            } else {
                for (XmlTag subTag : tag.getSubTags()) {
                    newTag.addSubTag(subTag,false);
                }
            }
            //delete old tag with empty parents
            while (tag.getParentTag() != null && tag.getParentTag().getSubTags().length == 1) {
                tag = tag.getParentTag();
            }
            tag.delete();
            return newTag;
        }
        return null;
    }

    public static boolean canRename(Project project, String oldName, String newName) {
        return newName != null
                && !newName.isEmpty()
                && !newName.equals(oldName)
                && EazeLocaleUtil.canCreateKey(project, newName);
    }

}
