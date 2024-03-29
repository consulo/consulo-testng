/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.theoryinpractice.testng.model;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.util.ClassUtil;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.util.TestNGUtil;
import consulo.application.ReadAction;
import consulo.execution.CantRunException;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.RuntimeConfigurationWarning;
import consulo.execution.configuration.ModuleBasedConfiguration;
import consulo.execution.test.SourceScope;
import consulo.execution.test.TestSearchScope;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class TestNGTestPattern extends TestNGTestObject
{
	private static final Logger LOG = Logger.getInstance(TestNGTestPattern.class);

	public TestNGTestPattern(TestNGConfiguration config)
	{
		super(config);
	}

	@Override
	public void fillTestObjects(Map<PsiClass, Map<PsiMethod, List<String>>> classes) throws CantRunException
	{
		final TestData data = myConfig.getPersistantData();
		final Set<String> patterns = data.getPatterns();
		fillTestObjects(classes, patterns, myConfig.getPersistantData().getScope(), myConfig, getSearchScope());
	}

	public static void fillTestObjects(final Map<PsiClass, Map<PsiMethod, List<String>>> classes,
			final Set<String> patterns,
			final TestSearchScope testSearchScope,
			final ModuleBasedConfiguration config,
			final GlobalSearchScope searchScope) throws CantRunException
	{
		for(final String pattern : patterns)
		{
			final String className;
			final String methodName;
			if(pattern.contains(","))
			{
				methodName = StringUtil.getShortName(pattern, ',');
				className = StringUtil.getPackageName(pattern, ',');
			}
			else
			{
				className = pattern;
				methodName = null;
			}

			final PsiClass psiClass = ReadAction.compute(() -> ClassUtil.findPsiClass(PsiManager.getInstance(config.getProject()), className.replace('/', '.'), null, true, searchScope));
			if(psiClass != null)
			{
				final Boolean hasTest = ReadAction.compute(() -> TestNGUtil.hasTest(psiClass));
				if(hasTest)
				{
					if(StringUtil.isEmpty(methodName))
					{
						calculateDependencies(null, classes, searchScope, psiClass);
					}
					else
					{
						collectTestMethods(classes, psiClass, methodName, searchScope);
					}
				}
				else
				{
					throw new CantRunException("No tests found in class " + className);
				}
			}
		}
		if(classes.size() != patterns.size())
		{
			final List<Pattern> compilePatterns = new ArrayList<>();
			for(String p : patterns)
			{
				final Pattern compilePattern;
				try
				{
					compilePattern = Pattern.compile(p);
				}
				catch(PatternSyntaxException e)
				{
					continue;
				}
				compilePatterns.add(compilePattern);
			}
			final SourceScope sourceScope = testSearchScope.getSourceScope(config);
			TestClassFilter projectFilter = new TestClassFilter(sourceScope != null ? sourceScope.getGlobalSearchScope() : GlobalSearchScope.allScope(config.getProject()), config.getProject(), true,
					true)
			{
				@Override
				public boolean isAccepted(PsiClass psiClass)
				{
					if(super.isAccepted(psiClass))
					{
						final String qualifiedName = ReadAction.compute(psiClass::getQualifiedName);
						LOG.assertTrue(qualifiedName != null);
						for(Pattern pattern : compilePatterns)
						{
							if(pattern.matcher(qualifiedName).matches())
							{
								return true;
							}
						}
					}
					return false;
				}
			};
			calculateDependencies(null, classes, searchScope, TestNGUtil.getAllTestClasses(projectFilter, false));
			if(classes.size() == 0)
			{
				throw new CantRunException("No tests found in for patterns \"" + StringUtil.join(patterns, " || ") + '\"');
			}
		}
	}

	@Override
	public String getGeneratedName()
	{
		final Set<String> patterns = myConfig.getPersistantData().getPatterns();
		final int size = patterns.size();
		if(size == 0)
		{
			return "Temp suite";
		}
		return StringUtil.getShortName(patterns.iterator().next()) + (size > 1 ? " and " + (size - 1) + " more" : "");
	}

	@Override
	public String getActionName()
	{
		return getGeneratedName();
	}

	@Override
	public void checkConfiguration() throws RuntimeConfigurationException
	{
		final Set<String> patterns = myConfig.getPersistantData().getPatterns();
		if(patterns.isEmpty())
		{
			throw new RuntimeConfigurationWarning("No pattern selected");
		}
	}
}
