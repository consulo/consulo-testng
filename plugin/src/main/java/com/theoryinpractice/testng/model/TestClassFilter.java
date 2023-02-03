// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.theoryinpractice.testng.model;

import com.intellij.java.execution.configurations.ConfigurationUtil;
import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.CommonClassNames;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiParameter;
import com.intellij.java.language.util.ClassFilter;
import com.theoryinpractice.testng.util.TestNGUtil;
import consulo.application.ReadAction;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;

import java.util.Arrays;
import java.util.List;

/**
 * @author Hani Suleiman
 */
public class TestClassFilter implements ClassFilter.ClassFilterWithScope
{
	private static final String GUICE = "org.testng.annotations.Guice";
	private static final List<String> INJECTION_ANNOTATIONS = Arrays.asList("com.google.inject.Inject", "org.testng.annotations.Factory");

	private final GlobalSearchScope scope;
	private final Project project;
	private final boolean includeConfig;
	private final boolean checkClassCanBeInstantiated;

	public TestClassFilter(GlobalSearchScope scope, Project project, boolean includeConfig)
	{
		this(scope, project, includeConfig, false);
	}

	public TestClassFilter(GlobalSearchScope scope, Project project, boolean includeConfig, boolean checkClassCanBeInstantiated)
	{
		this.scope = scope;
		this.project = project;
		this.includeConfig = includeConfig;
		this.checkClassCanBeInstantiated = checkClassCanBeInstantiated;
	}

	public TestClassFilter intersectionWith(GlobalSearchScope scope)
	{
		return new TestClassFilter(this.scope.intersectWith(scope), project, includeConfig, checkClassCanBeInstantiated);
	}

	public boolean isAccepted(final PsiClass psiClass)
	{
		return ReadAction.compute(() ->
		{
			if(!ConfigurationUtil.PUBLIC_INSTANTIATABLE_CLASS.test(psiClass))
			{
				return false;
			}
			//PsiManager manager = PsiManager.getInstance(project);
			//if(manager.getEffectiveLanguageLevel().compareTo(LanguageLevel.JDK_1_5) < 0) return true;
			boolean hasTest = TestNGUtil.hasTest(psiClass);
			if(hasTest)
			{
				if(checkClassCanBeInstantiated)
				{
					final PsiMethod[] constructors = psiClass.getConstructors();
					if(constructors.length > 0)
					{
						boolean canBeInstantiated = false;
						for(PsiMethod constructor : constructors)
						{
							PsiParameter[] parameters = constructor.getParameterList().getParameters();
							if(parameters.length == 0 || AnnotationUtil.isAnnotated(constructor, INJECTION_ANNOTATIONS, AnnotationUtil.CHECK_HIERARCHY) || parameters.length == 1 && parameters[0]
									.getType().equalsToText(CommonClassNames.JAVA_LANG_STRING))
							{
								canBeInstantiated = true;
								break;
							}
						}
						if(!canBeInstantiated && !AnnotationUtil.isAnnotated(psiClass, GUICE, 0))
						{
							return false;
						}
					}
				}
				return true;
			}

			return includeConfig && TestNGUtil.hasConfig(psiClass, TestNGUtil.CONFIG_ANNOTATIONS_FQN_NO_TEST_LEVEL);
		});
	}

	public Project getProject()
	{
		return project;
	}

	public GlobalSearchScope getScope()
	{
		return scope;
	}
}