/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.theoryinpractice.testng;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiJavaFile;
import com.intellij.java.language.psi.util.PsiClassUtil;
import com.theoryinpractice.testng.inspection.TestNGSearchScope;
import com.theoryinpractice.testng.util.TestNGUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.navigation.GotoRelatedProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.search.PsiNonJavaFileReferenceProcessor;
import consulo.language.psi.search.PsiSearchHelper;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.xml.psi.xml.XmlAttribute;
import consulo.xml.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: anna
 */
@ExtensionImpl
public class TestNGRelatedFilesProvider extends GotoRelatedProvider
{
	@NotNull
	@Override
	public List<? extends GotoRelatedItem> getItems(@NotNull PsiElement context)
	{
		PsiClass psiClass = PsiTreeUtil.getParentOfType(context, PsiClass.class, false);
		if(psiClass != null)
		{
			final Project project = psiClass.getProject();
			while(psiClass != null && TestNGUtil.hasTest(psiClass) && PsiClassUtil.isRunnableClass(psiClass, true))
			{
				final String qName = psiClass.getQualifiedName();
				if(qName != null)
				{
					final String packageQName = ((PsiJavaFile) psiClass.getContainingFile()).getPackageName();
					final String packageName = StringUtil.getShortName(packageQName);
					final String[] names;
					if(packageQName.length() > 0)
					{
						final String pName = packageName.length() > 0 ? packageName : packageQName;
						names = new String[]{
								qName,
								pName
						};
					}
					else
					{
						names = new String[]{qName};
					}
					final List<PsiElement> tags = new ArrayList<PsiElement>();
					for(final String name : names)
					{
						PsiSearchHelper.SERVICE.getInstance(project)
								.processUsagesInNonJavaFiles(name, new PsiNonJavaFileReferenceProcessor()
								{
									public boolean process(final PsiFile file, final int startOffset, final int endOffset)
									{
										final PsiReference referenceAt = file.findReferenceAt(startOffset);
										if(referenceAt != null)
										{
											if(packageQName.endsWith(name))
											{ //special package tag required
												final XmlTag tag = PsiTreeUtil.getParentOfType(file.findElementAt(startOffset), XmlTag.class);
												if(tag == null || !tag.getName().equals("package"))
												{
													return true;
												}
												final XmlAttribute attribute = tag.getAttribute("name");
												if(attribute == null)
												{
													return true;
												}
												final String value = attribute.getValue();
												if(value == null)
												{
													return true;
												}
												if(!(value.equals(StringUtil.getQualifiedName(packageQName, "*")) || value.equals(packageQName)))
												{
													return true;
												}
											}
											tags.add(referenceAt.getElement());
										}
										return true;
									}
								}, new TestNGSearchScope(project));
					}

					if(!tags.isEmpty())
					{
						return GotoRelatedItem.createItems(tags, "TestNG");
					}
				}
				psiClass = PsiTreeUtil.getParentOfType(psiClass, PsiClass.class);
			}
		}
		return Collections.emptyList();
	}
}
