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
package com.theoryinpractice.testng.inspection;

import com.intellij.java.analysis.impl.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.InheritanceUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import jakarta.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Bas Leijdekkers
 */
@ExtensionImpl
public class AssertsWithoutMessagesTestNGInspection extends BaseJavaLocalInspectionTool
{
	@NotNull
	@Override
	public PsiElementVisitor buildVisitorImpl(@Nonnull ProblemsHolder holder, boolean isOnTheFly, LocalInspectionToolSession session, Object o)
	{
		return new AssertionsWithoutMessagesVisitor(holder);
	}

	@Override
	public boolean isEnabledByDefault()
	{
		return false;
	}

	@Nonnull
	@Override
	public String getDisplayName()
	{
		return "Message missing on TestNG assertion";
	}

	@Nonnull
	@Override
	public String getGroupDisplayName()
	{
		return "TestNG";
	}

	private static class AssertionsWithoutMessagesVisitor extends JavaElementVisitor
	{

		@NonNls
		private static final Set<String> ourAssertMethods = new HashSet<String>(10);
		private final ProblemsHolder myProblemsHolder;

		public AssertionsWithoutMessagesVisitor(ProblemsHolder problemsHolder)
		{
			myProblemsHolder = problemsHolder;
		}

		static
		{
			ourAssertMethods.add("assertArrayEquals");
			ourAssertMethods.add("assertEquals");
			ourAssertMethods.add("assertEqualsNoOrder");
			ourAssertMethods.add("assertFalse");
			ourAssertMethods.add("assertNotEquals");
			ourAssertMethods.add("assertNotNull");
			ourAssertMethods.add("assertNotSame");
			ourAssertMethods.add("assertNull");
			ourAssertMethods.add("assertSame");
			ourAssertMethods.add("assertTrue");
			ourAssertMethods.add("fail");
		}

		@Override
		public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression)
		{
			super.visitMethodCallExpression(expression);
			final PsiReferenceExpression methodExpression = expression.getMethodExpression();
			final String methodName = methodExpression.getReferenceName();
			if(methodName == null || !ourAssertMethods.contains(methodName))
			{
				return;
			}
			final PsiMethod method = expression.resolveMethod();
			if(method == null)
			{
				return;
			}
			final PsiClass containingClass = method.getContainingClass();
			final boolean messageFirst;
			if(InheritanceUtil.isInheritor(containingClass, "org.testng.AssertJUnit"))
			{
				messageFirst = true;
			}
			else if(InheritanceUtil.isInheritor(containingClass, "org.testng.Assert"))
			{
				messageFirst = false;
			}
			else
			{
				return;
			}
			final PsiParameterList parameterList = method.getParameterList();
			final int parameterCount = parameterList.getParametersCount();
			if(parameterCount < 2 && methodName.startsWith("assert"))
			{
				registerMethodCallError(expression);
				return;
			}
			if(parameterCount < 1)
			{
				registerMethodCallError(expression);
				return;
			}
			final PsiManager psiManager = expression.getManager();
			final Project project = psiManager.getProject();
			final GlobalSearchScope scope = GlobalSearchScope.allScope(project);
			final PsiType stringType = PsiType.getJavaLangString(psiManager, scope);
			final PsiParameter[] parameters = parameterList.getParameters();
			if(messageFirst)
			{
				final PsiType parameterType1 = parameters[0].getType();
				if(!stringType.equals(parameterType1))
				{
					registerMethodCallError(expression);
					return;
				}
				if(parameters.length == 2)
				{
					final PsiType parameterType2 = parameters[1].getType();
					if(stringType.equals(parameterType2))
					{
						registerMethodCallError(expression);
					}
				}
			}
			else
			{
				final PsiType lastParameterType = parameters[parameters.length - 1].getType();
				if(!stringType.equals(lastParameterType))
				{
					registerMethodCallError(expression);
					return;
				}
				if(parameters.length == 2)
				{
					final PsiType firstParameterType = parameters[0].getType();
					if(stringType.equals(firstParameterType))
					{
						registerMethodCallError(expression);
					}
				}
			}
		}

		private void registerMethodCallError(PsiMethodCallExpression expression)
		{
			final PsiReferenceExpression methodExpression = expression.getMethodExpression();
			final PsiElement nameElement = methodExpression.getReferenceNameElement();
			if(nameElement == null)
			{
				myProblemsHolder.registerProblem(methodExpression, "TestNG <code>#ref()</code> without message #loc");
			}
			else
			{
				myProblemsHolder.registerProblem(nameElement, "TestNG <code>#ref()</code> without message #loc");
			}
		}
	}
}