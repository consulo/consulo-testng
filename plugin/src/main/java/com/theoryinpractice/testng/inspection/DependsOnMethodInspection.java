/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.theoryinpractice.testng.inspection;

import com.intellij.java.analysis.impl.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.java.language.psi.*;
import com.theoryinpractice.testng.util.TestNGUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Hani Suleiman Date: Aug 3, 2005 Time: 3:34:56 AM
 */
@ExtensionImpl
public class DependsOnMethodInspection extends BaseJavaLocalInspectionTool<Object>
{
	private static final Logger LOGGER = Logger.getInstance("TestNG Runner");
	private static final Pattern PATTERN = Pattern.compile("\"([a-zA-Z1-9_\\(\\)]*)\"");
	private static final ProblemDescriptor[] EMPTY = new ProblemDescriptor[0];

	@NotNull
	@Override
	public String getGroupDisplayName()
	{
		return "TestNG";
	}

	@NotNull
	@Override
	public String getDisplayName()
	{
		return "dependsOnMethods problem";
	}

	@NotNull
	@Override
	public String getShortName()
	{
		return "dependsOnMethodTestNG";
	}

	public boolean isEnabledByDefault()
	{
		return true;
	}

	@Override
	@Nullable
	public ProblemDescriptor[] checkClass(@NotNull PsiClass psiClass, @NotNull InspectionManager manager, boolean isOnTheFly, Object state)
	{
		//LOGGER.info("Looking for dependsOnMethods problems in " + psiClass.getName());

		if(!psiClass.getContainingFile().isWritable())
		{
			return null;
		}

		PsiAnnotation[] annotations = TestNGUtil.getTestNGAnnotations(psiClass);
		if(annotations.length == 0)
		{
			return EMPTY;
		}
		List<ProblemDescriptor> problemDescriptors = new ArrayList<ProblemDescriptor>();

		for(PsiAnnotation annotation : annotations)
		{
			PsiNameValuePair dep = null;
			PsiNameValuePair[] params = annotation.getParameterList().getAttributes();
			for(PsiNameValuePair param : params)
			{
				if("dependsOnMethods".equals(param.getName()))
				{
					dep = param;
					break;
				}
			}

			if(dep != null)
			{
				if(dep.getValue() != null)
				{
					final PsiAnnotationMemberValue value = dep.getValue();
					if(value != null)
					{
						String text = value.getText();
						if(value instanceof PsiReferenceExpression)
						{
							final PsiElement resolve = ((PsiReferenceExpression) value).resolve();
							if(resolve instanceof PsiField && ((PsiField) resolve).hasModifierProperty(PsiModifier.STATIC) && ((PsiField) resolve).hasModifierProperty(PsiModifier.FINAL))
							{
								final PsiExpression initializer = ((PsiField) resolve).getInitializer();
								if(initializer != null)
								{
									text = initializer.getText();
								}
							}
						}
						Matcher matcher = PATTERN.matcher(text);
						while(matcher.find())
						{
							String methodName = matcher.group(1);
							checkMethodNameDependency(manager, psiClass, methodName, dep, problemDescriptors, isOnTheFly);
						}
					}
				}
			}
		}

		return problemDescriptors.toArray(new ProblemDescriptor[]{});
	}

	private static void checkMethodNameDependency(InspectionManager manager, PsiClass psiClass, String methodName, PsiNameValuePair dep,
												  List<ProblemDescriptor> problemDescriptors, boolean onTheFly)
	{
		LOGGER.debug("Found dependsOnMethods with text: " + methodName);
		if(methodName.length() > 0 && methodName.charAt(methodName.length() - 1) == ')')
		{

			LOGGER.debug("dependsOnMethods contains ()" + psiClass.getName());
			// TODO Add quick fix for removing brackets on annotation
			ProblemDescriptor descriptor = manager.createProblemDescriptor(dep,
					"Method '" + methodName + "' should not include () characters.",
					(LocalQuickFix) null,
					ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, onTheFly);

			problemDescriptors.add(descriptor);

		}
		else
		{
			PsiMethod[] foundMethods = psiClass.findMethodsByName(methodName, true);

			if(foundMethods.length == 0)
			{
				LOGGER.debug("dependsOnMethods method doesn't exist:" + methodName);
				ProblemDescriptor descriptor = manager.createProblemDescriptor(dep,
						"Method '" + methodName + "' unknown.",
						(LocalQuickFix) null,
						ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, onTheFly);
				problemDescriptors.add(descriptor);

			}
			else
			{
				boolean hasTestsOrConfigs = false;
				for(PsiMethod foundMethod : foundMethods)
				{
					hasTestsOrConfigs |= TestNGUtil.hasTest(foundMethod) || TestNGUtil.hasConfig(foundMethod);
				}
				if(!hasTestsOrConfigs)
				{
					ProblemDescriptor descriptor = manager.createProblemDescriptor(dep,
							"Method '" + methodName + "' is not a test or configuration method.",
							(LocalQuickFix) null,
							ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, onTheFly);
					problemDescriptors.add(descriptor);
				}
			}
		}
	}

}
