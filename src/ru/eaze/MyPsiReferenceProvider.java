package ru.eaze;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.*;
import com.intellij.util.ProcessingContext;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.diagnostic.Logger;
import ru.eaze.domain.EazeProjectStructure;
import ru.eaze.reference.EazeActionPhpReference;
import ru.eaze.reference.EazeActionReference;
import ru.eaze.reference.EazeUriReference;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: user
 * Date: 26.01.12
 * Time: 1:11
 *
 * Этот класс предоставляет список ссылок, привязанных к данному элементу (PsiElement)
 * В качестве PsiElement выступает XmlTagElement в pages.xml
 */
public class MyPsiReferenceProvider extends PsiReferenceProvider {
     public static final PsiReferenceProvider[] EMPTY_ARRAY = new PsiReferenceProvider[0];
    HashMap<Project, EazeProjectStructure> structs = new HashMap<Project, EazeProjectStructure>();
    //protected ru.eaze.domain.EazeProjectStructure projectStructure;

    static class StringMatch {
        private String name;
        private  int offset;

        public StringMatch( String name, int offset ){
            this.offset = offset;
            this.name = name;
        }
        public String getName() {
            return name;
        }
        public int getStartOffset( ){
            return  offset;
        }
        public int getEndOffset(){
            return offset + name.length();
        }

        @Override
        public String toString() {
            return name;
        }
        public int getLength(){
            return name.length();
        }
    }

    public EazeProjectStructure getEazeStructure(Project project) {
        if ( structs.containsKey( project )) {
            return structs.get( project );
        }
        VirtualFile baseDir = project.getBaseDir();
        if (baseDir != null) {
            VirtualFile webDir = baseDir.findFileByRelativePath("web/");
            if (webDir != null) {
                EazeProjectStructure projectStructure = new EazeProjectStructure(project, webDir);
                structs.put( project, projectStructure );
                return projectStructure;
            }
        }
        return null;
    }
    public  MyPsiReferenceProvider(){
    }
      @NotNull @Override
      public  PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull final ProcessingContext context) {
          Project project = element.getProject();
          PsiFile psiFile = element.getContainingFile();
              String fileName = psiFile.getName();
             // System.out.println( psiFile.getName() );
              boolean isPagesXml = fileName.equals("pages.xml");
          if ( element instanceof XmlTag ) {
              XmlTag tag = (XmlTag)element;

            if ( isPagesXml && tag.getName().equals("template") ) {
                XmlText[] textElements = tag.getValue().getTextElements();
                if (textElements.length != 0) {
                    XmlText text = textElements[0];
                    int offset = text.getStartOffsetInParent();
                    System.out.println(text.getText());
                    PsiReference ref =   new EazeUriReference(text.getText(),/*textElements[0] */tag, new TextRange(offset, offset+text.getTextLength()), getEazeStructure(project), project);
                     return new PsiReference[]{ ref };
                }
            } else if (tag.getName().equals("actions") || ( tag.getName().equals("action") && isPagesXml ) ) {
                XmlText[] textElements = tag.getValue().getTextElements();
                if (textElements.length != 0) {
                    XmlText text = textElements[0];
                    String actions = text.getText();
                    System.out.println( text.getText());
                    StringMatch[] pageActions = getActionNamesFromString( actions );
                     int offset = text.getStartOffsetInParent();
                      ArrayList<PsiReference> refs = new ArrayList<PsiReference>();
                    for( StringMatch action : pageActions){

                        int start = offset + action.getStartOffset();
                        int len  = action.getLength();
                        PsiReference ref =   new EazeActionReference(action.toString(), tag , new TextRange(start, start + len), getEazeStructure(project), project);
                         refs.add(ref);
                    }

                    if ( refs.size() != 0 ) {
                       return refs.toArray(new PsiReference[0]);
                    }
                }
            } else if ( tag.getName().equals("action")) {
                XmlTag pathTag = tag.findFirstSubTag("path");
                String actionName = tag.getAttributeValue("name");
                PsiFile file =  tag.getContainingFile();
                VirtualFile virtualFile =  file.getVirtualFile();
                if ( file != null ){
                    String packageName = file.getName().replace(".xml", "");
                    actionName = packageName + '.' + actionName;
                    int offset = 0;
                    int length = 0;
                    if ( pathTag != null ){
                        offset = pathTag.getStartOffsetInParent();
                    }
                }

            } else if (tag.getName().equals("path")) {
                XmlTag parentTag = tag.getParentTag();
                XmlTagValue xmlTagValue = tag.getValue();
                if ((parentTag != null) && xmlTagValue != null) {
                    XmlText[] textElements = tag.getValue().getTextElements();
                    if (textElements.length != 0) {
                        XmlText text = textElements[0];
                        int offset = text.getStartOffsetInParent();
                        if (parentTag.getName().equals("action") && text.getTextLength() != 0) {
                            PsiReference ref = new EazeActionPhpReference(parentTag, text.getText(), tag, new TextRange(offset, offset + text.getTextLength()), getEazeStructure(project), project);
                            return new PsiReference[]{ref};
                        }
                    }
                }
            }
          }    if ( element instanceof XmlAttributeValue ) {
               XmlAttributeValue attributeValue = (XmlAttributeValue) element;
              PsiElement parent= attributeValue.getParent();
              if ( parent != null && parent instanceof XmlAttribute ) {
                  XmlAttribute nameAttribute = (XmlAttribute) parent;
                  if ( nameAttribute.getName().equals("name") ) {
                      XmlTag parentTag =  (XmlTag) parent.getParent();
                      if ( parentTag.getName().equals("action") ) {
                          VirtualFile file = EazeProjectStructure.GetFileByActionTag( parentTag );
                          if ( file != null ) {  // чтобы не подсвечивало красным сразу две строчки
                              int offset = attributeValue.getValueTextRange().getStartOffset();
                              offset -= attributeValue.getTextRange().getStartOffset();
                              int length = attributeValue.getValueTextRange().getLength();
                              PsiReference ref =   new EazeActionPhpReference(parentTag,"somethin", attributeValue , new TextRange(offset, offset + length), getEazeStructure(project) , project);
                              return new PsiReference[]{ ref };
                          }
                      }
                  } else if ( isPagesXml && nameAttribute.getName().equals("shutdown") || nameAttribute.getName().equals("boot")) {
                    String actions = nameAttribute.getValue();
                      if ( actions.length() == 0) {
                          return  new PsiReference[]{};
                      }
                    ArrayList<PsiReference> refs = new ArrayList<PsiReference>();
                      StringMatch[] pageActions = getActionNamesFromString( actions );
                    int offset = 0;
                      if ( pageActions.length == 0 ) {
                          return new PsiReference[]{};
                      }
                        int attribValueOffset = attributeValue.getValueTextRange().getStartOffset();
                              attribValueOffset -= attributeValue.getTextRange().getStartOffset();
                    for( StringMatch action : pageActions){
                        int start = attribValueOffset + offset + action.getStartOffset();
                        int len  = action.getLength();
                        PsiReference ref =   new EazeActionReference(action.toString(), attributeValue , new TextRange(start, start + len), getEazeStructure(project), project);
                         refs.add(ref);
                    }
                      if ( refs.size() != 0 ) {
                       return refs.toArray(new PsiReference[0]);
                      }
                  }
              }

          }  if ( element instanceof XmlToken ) {
              XmlToken xmlToken = (XmlToken)element;
              IElementType obj = xmlToken.getTokenType();
              if ( obj.toString().equals("XML_DATA_CHARACTERS") ) {
                  String text = xmlToken.getText();

                  int startIndex = 0;
                   ArrayList<PsiReference> refs = new ArrayList<PsiReference>();
                  while (true) {
                      int index = text.indexOf("{increal:", startIndex);
                      if (index == -1) {
                          break;
                      }
                      index += 9;
                      int closingBracketIndex = text.indexOf("}", index);
                      if (closingBracketIndex == -1) {
                          break;
                      }
                      startIndex = closingBracketIndex + 1;

                      String uri = text.substring( index, closingBracketIndex );
                      PsiReference ref = new EazeUriReference(uri, element , new TextRange(index, closingBracketIndex), getEazeStructure(project), project);
                      refs.add(ref);
                  }
                      if ( refs.size() != 0 ) {
                        return refs.toArray(new PsiReference[0]);
                      }
              }
          }
          else {
              String className = element.getClass().getName();
              Class elementClass = element.getClass();
              if (className.endsWith("StringLiteralExpressionImpl")) {

                  try {
                      Method method = elementClass.getMethod("getValueRange");
                      Object obj = method.invoke(element);
                      TextRange textRange = (TextRange) obj;
                      Class _PhpPsiElement = elementClass.getSuperclass().getSuperclass().getSuperclass();
                      Method phpPsiElementGetText = _PhpPsiElement.getMethod("getText");
                      // Method method2 = elementClass.getMethod("getValue");
                      Object obj2 = phpPsiElementGetText.invoke(element);
                      String str = obj2.toString();
                      String uri = str.substring( textRange.getStartOffset(), textRange.getEndOffset());
                       int start = textRange.getStartOffset();
                      int len = textRange.getLength();
                      if ( getEazeStructure(project).LooksLikeEazeUri( uri )) {
                           PsiReference ref = new EazeUriReference(uri, element , new TextRange(start, start + len), getEazeStructure(project), project);
                          return new PsiReference[]{ ref };
                      }

                  } catch (Exception e) {
                     // log.warn("INVOKEERROR " + e.getClass().toString());
                  }
              }
          }
           return PsiReference.EMPTY_ARRAY;
          //return new PsiReference[]{};
      }

    public static void PrintElementClassDescription(Object element) {
        Logger log = Logger.getInstance("ERROR");
        String classDescription = "";
        Class parentclass = element.getClass();
        do {
            classDescription += "\r\nPSICLASS " + parentclass.toString() + "\r\n";

            Class[] intefaces = parentclass.getInterfaces();
            for (Class interfac : intefaces) {
                classDescription += "\r\nIMPLEMENTS INTERFACE " + interfac.getName() + "\r\n";
            }

            //Get the methods
            Method[] methods = parentclass.getDeclaredMethods();

            //Loop through the methods and print out their names
            for (Method method : methods) {
                String singature = method.getName() + "(";
                Class[] params = method.getParameterTypes();
                for (Class clas : params) {
                    singature += clas.getName() + ",";
                }
                singature += ")";
                classDescription += "\r\n PSICLASS method: " + singature;
            }
            parentclass = parentclass.getSuperclass();


        } while (parentclass != null);
        log.warn(classDescription);
    }

    public static StringMatch[] getActionNamesFromString( String actions ){
        int startIndex = 0;
        ArrayList<StringMatch> refs = new ArrayList<StringMatch>();
        while (true) {
            String actionName = "";
            int index = actions.indexOf(",", startIndex);
            if (index == -1) {
                 actionName = actions.substring( startIndex, actions.length() );
                 if ( actionName.length() == 0 ){
                     break;
                 }
            }
            else  {
                actionName = actions.substring( startIndex, index );
            }

            String trimmedActionName = actionName.trim();
            int offset = actionName.indexOf( trimmedActionName );

            StringMatch match = new StringMatch( trimmedActionName, offset + startIndex );
            refs.add( match );
            startIndex = index+1;
            if (index == -1) {
                break;
            }
        }
        return refs.toArray(new StringMatch [0]);
    }
}
