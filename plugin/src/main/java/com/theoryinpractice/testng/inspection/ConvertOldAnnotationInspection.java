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

/*
 * Created by IntelliJ IDEA.
 * User: amrk
 * Date: 11/09/2006
 * Time: 19:50:29
 */
package com.theoryinpractice.testng.inspection;

import com.intellij.java.analysis.impl.codeInspection.BaseJavaLocalInspectionTool;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.codeStyle.JavaCodeStyleManager;
import com.theoryinpractice.testng.util.TestNGUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author Hani Suleiman Date: Aug 3, 2005 Time: 4:17:59 PM
 */
@ExtensionImpl
public class ConvertOldAnnotationInspection extends BaseJavaLocalInspectionTool
{
	private static final String DISPLAY_NAME = "Convert old @Configuration TestNG annotations";

	@Override
	public boolean isEnabledByDefault()
	{
		return false;
	}

	@Nls
	@NotNull
	public String getGroupDisplayName()
	{
		return TestNGUtil.TESTNG_GROUP_NAME;
	}

	@Nls
	@NotNull
	public String getDisplayName()
	{
		return DISPLAY_NAME;
	}

	@NonNls
	@NotNull
	public String getShortName()
	{
		return "ConvertOldAnnotations";
	}

	@NotNull
	public PsiElementVisitor buildVisitorImpl(@Nonnull ProblemsHolder holder, boolean isOnTheFly, LocalInspectionToolSession session, Object o)
	{
		return new JavaElementVisitor()
		{
			@Override
			public void visitAnnotation(final PsiAnnotation annotation)
			{
				final String qualifiedName = annotation.getQualifiedName();
				if(Comparing.strEqual(qualifiedName, "org.testng.annotations.Configuration"))
				{
					holder.registerProblem(annotation, DISPLAY_NAME, new ConvertOldAnnotationsQuickfix());
				}
			}
		};
	}

	private static class ConvertOldAnnotationsQuickfix implements LocalQuickFix
	{
		private static final Logger LOG = Logger.getInstance("#" + ConvertOldAnnotationsQuickfix.class.getName());

		@NotNull
		public String getName()
		{
			return DISPLAY_NAME;
		}

		@NotNull
		public String getFamilyName()
		{
			return getName();
		}

		public void applyFix(@NotNull final Project project, @NotNull final ProblemDescriptor descriptor)
		{
			final PsiAnnotation annotation = (PsiAnnotation) descriptor.getPsiElement();
			if(!TestNGUtil.checkTestNGInClasspath(annotation))
			{
				return;
			}
			final PsiModifierList modifierList = PsiTreeUtil.getParentOfType(annotation, PsiModifierList.class);
			LOG.assertTrue(modifierList != null);
			try
			{
				convertOldAnnotationAttributeToAnnotation(modifierList, annotation, "beforeTest", "@org.testng.annotations.BeforeTest");
				convertOldAnnotationAttributeToAnnotation(modifierList, annotation, "beforeTestClass", "@org.testng.annotations.BeforeTest");
				convertOldAnnotationAttributeToAnnotation(modifierList, annotation, "beforeTestMethod",
						"@org.testng.annotations.BeforeMethod");
				convertOldAnnotationAttributeToAnnotation(modifierList, annotation, "beforeSuite", "@org.testng.annotations.BeforeSuite");
				convertOldAnnotationAttributeToAnnotation(modifierList, annotation, "beforeGroups", "@org.testng.annotations.BeforeGroups");
				convertOldAnnotationAttributeToAnnotation(modifierList, annotation, "afterTest", "@org.testng.annotations.AfterTest");
				convertOldAnnotationAttributeToAnnotation(modifierList, annotation, "afterTestClass", "@org.testng.annotations.AfterTest");
				convertOldAnnotationAttributeToAnnotation(modifierList, annotation, "afterTestMethod", "@org.testng.annotations.AfterMethod");
				convertOldAnnotationAttributeToAnnotation(modifierList, annotation, "afterSuite", "@org.testng.annotations.AfterSuite");
				convertOldAnnotationAttributeToAnnotation(modifierList, annotation, "afterGroups", "@org.testng.annotations.AfterGroups");
				annotation.delete();
			}
			catch(IncorrectOperationException e)
			{
				LOG.error(e);
			}
		}
	}

	private static void convertOldAnnotationAttributeToAnnotation(PsiModifierList modifierList,
																  PsiAnnotation annotation,
																  @NonNls String attribute,
																  @NonNls String newAnnotation) throws IncorrectOperationException
	{

		PsiAnnotationParameterList list = annotation.getParameterList();
		for(PsiNameValuePair pair : list.getAttributes())
		{
			if(attribute.equals(pair.getName()))
			{
				final StringBuffer newAnnotationBuffer = new StringBuffer();
				newAnnotationBuffer.append(newAnnotation).append('(').append(')');
				final PsiElementFactory factory = JavaPsiFacade.getInstance(annotation.getProject()).getElementFactory();
				final PsiAnnotation newPsiAnnotation = factory.createAnnotationFromText(newAnnotationBuffer.toString(), modifierList);
				JavaCodeStyleManager.getInstance(annotation.getProject()).shortenClassReferences(modifierList.addAfter(newPsiAnnotation, null));
			}
		}
	}

}
