package ru.eaze;

import com.intellij.patterns.*;
import com.intellij.psi.*;
import com.intellij.psi.xml.*;
//import com.intellij.patterns.X

/**
 * Created by IntelliJ IDEA.
 * User: user
 * Date: 27.01.12
 * Time: 23:06
 * To change this template use File | Settings | File Templates.
 */

public class MyPsiReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
     MyPsiReferenceProvider provider = new MyPsiReferenceProvider();
      //  PsiReferenceRegistrar  reg = PsiReferenceRegistrar.
     /*   ReferenceProvidersRegistry registry = new ReferenceProvidersRegistry(project);  */
        registrar.registerReferenceProvider(StandardPatterns.instanceOf(XmlAttributeValue.class), provider);
        registrar.registerReferenceProvider(StandardPatterns.instanceOf(XmlTag.class), provider);
        //StandardPatterns.instanceOf(String.class)
        //registrar.registerReferenceProvider(StandardPatterns.instanceOf(PsiLiteralExpression.class), provider, 100);
        registrar.registerReferenceProvider(StandardPatterns.instanceOf(PsiElement.class), provider);
/*+    XmlUtil.registerXmlAttributeValueReferenceProvider(registrar, new String[]{
+      "name"
+    }, new PatternFilter(xmlAttributeValue().withParent(NAME_PATTERN)), true, new PrefixReferenceProvider());
+
+//    final XmlAttributeValuePattern id = xmlAttributeValue().withParent(xmlAttribute()).with(IdRefProvider.HAS_ID_REF_TYPE);
+//    final XmlAttributeValuePattern idref = xmlAttributeValue().withParent(xmlAttribute()).with(IdRefProvider.HAS_ID_TYPE);
+//    registry.registerXmlAttributeValueReferenceProvider(null, new PatternFilter(or(id, idref)), false, new IdRefProvider());
+
+  } */
 }
}
