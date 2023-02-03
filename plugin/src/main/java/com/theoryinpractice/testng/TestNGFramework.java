// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.theoryinpractice.testng;

import com.intellij.java.analysis.impl.codeInsight.intention.AddAnnotationFix;
import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.projectRoots.roots.ExternalLibraryDescriptor;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.java.language.testIntegration.JavaTestFramework;
import com.theoryinpractice.testng.configuration.TestNGConfigurationType;
import com.theoryinpractice.testng.intention.TestNGExternalLibraryResolver;
import com.theoryinpractice.testng.util.TestNGUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ApplicationManager;
import consulo.application.CommonBundle;
import consulo.execution.configuration.ConfigurationType;
import consulo.fileTemplate.FileTemplateDescriptor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.util.IncorrectOperationException;
import consulo.testng.icon.TestNGIconGroup;
import consulo.ui.ex.awt.Messages;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

@ExtensionImpl
public class TestNGFramework extends JavaTestFramework
{
	private final static List<String> SECONDARY_BEFORE_ANNOTATIONS = Arrays.asList("org.testng.annotations.BeforeTest", "org.testng.annotations.BeforeClass", "org.testng.annotations.BeforeSuite",
			"org.testng.annotations.BeforeGroups");

	@Override
	@NotNull
	public String getName()
	{
		return "TestNG";
	}

	@NotNull
	@Override
	public Image getIcon()
	{
		return TestNGIconGroup.testng();
	}

	@Override
	protected String getMarkerClassFQName()
	{
		return "org.testng.annotations.Test";
	}

	@Override
	public ExternalLibraryDescriptor getFrameworkLibraryDescriptor()
	{
		return TestNGExternalLibraryResolver.TESTNG_DESCRIPTOR;
	}

	@Override
	@Nullable
	public String getDefaultSuperClass()
	{
		return null;
	}

	@Override
	public boolean isTestClass(PsiClass clazz, boolean canBePotential)
	{
		if(canBePotential)
		{
			return isUnderTestSources(clazz);
		}
		return TestNGUtil.isTestNGClass(clazz);
	}

	@Nullable
	@Override
	protected PsiMethod findSetUpMethod(@NotNull PsiClass clazz)
	{
		for(PsiMethod each : clazz.getMethods())
		{
			if(AnnotationUtil.isAnnotated(each, "org.testng.annotations.BeforeMethod", 0))
			{
				return each;
			}
		}
		return null;
	}

	@Nullable
	@Override
	protected PsiMethod findTearDownMethod(@NotNull PsiClass clazz)
	{
		for(PsiMethod each : clazz.getMethods())
		{
			if(AnnotationUtil.isAnnotated(each, "org.testng.annotations.AfterMethod", 0))
			{
				return each;
			}
		}
		return null;
	}

	@Override
	protected PsiMethod findOrCreateSetUpMethod(PsiClass clazz) throws IncorrectOperationException
	{
		PsiMethod method = findSetUpMethod(clazz);
		if(method != null)
		{
			return method;
		}

		final PsiManager manager = clazz.getManager();
		final PsiElementFactory factory = JavaPsiFacade.getInstance(manager.getProject()).getElementFactory();
		String setUpName = "setUp";
		PsiMethod patternMethod = createSetUpPatternMethod(factory);
		PsiMethod inClass = clazz.findMethodBySignature(patternMethod, false);
		if(inClass != null)
		{
			if(AnnotationUtil.isAnnotated(inClass, SECONDARY_BEFORE_ANNOTATIONS, 0))
			{
				return inClass;
			}
			int exit = ApplicationManager.getApplication().isUnitTestMode() ? Messages.YES : Messages.showYesNoDialog(manager.getProject(), "Method \'" + setUpName + "\' already exist but is not " +
					"annotated as @BeforeMethod.", CommonBundle.getWarningTitle(), "Annotate", "Create new method", Messages.getWarningIcon());
			if(exit == Messages.YES)
			{
				new AddAnnotationFix(BeforeMethod.class.getName(), inClass).invoke(inClass.getProject(), null, inClass.getContainingFile());
				return inClass;
			}
			else if(exit == Messages.NO)
			{
				inClass = null;
				int i = 0;
				while(clazz.findMethodBySignature(patternMethod, false) != null)
				{
					patternMethod.setName(setUpName + (++i));
				}
				setUpName = patternMethod.getName();
			}
		}

		final PsiClass superClass = clazz.getSuperClass();
		if(superClass != null)
		{
			final PsiMethod[] methods = superClass.findMethodsBySignature(patternMethod, false);
			if(methods.length > 0)
			{
				final PsiModifierList modifierList = methods[0].getModifierList();
				if(!modifierList.hasModifierProperty(PsiModifier.PRIVATE))
				{ //do not override private method
					@NonNls String pattern = "@" + BeforeMethod.class.getName() + "\n";
					if(modifierList.hasModifierProperty(PsiModifier.PROTECTED))
					{
						pattern += "protected ";
					}
					else if(modifierList.hasModifierProperty(PsiModifier.PUBLIC))
					{
						pattern += "public ";
					}
					patternMethod = factory.createMethodFromText(pattern + "void " + setUpName + "() throws Exception {\nsuper." + setUpName + "();\n}", null);
				}
			}
		}

		final PsiMethod[] psiMethods = clazz.getMethods();

		PsiMethod testMethod = null;
		for(PsiMethod psiMethod : psiMethods)
		{
			if(inClass == null && AnnotationUtil.isAnnotated(psiMethod, BeforeMethod.class.getName(), 0))
			{
				inClass = psiMethod;
			}
			if(testMethod == null && AnnotationUtil.isAnnotated(psiMethod, Test.class.getName(), 0) && !psiMethod.hasModifierProperty(PsiModifier.PRIVATE))
			{
				testMethod = psiMethod;
			}
		}
		if(inClass == null)
		{
			final PsiMethod psiMethod;
			if(testMethod != null)
			{
				psiMethod = (PsiMethod) clazz.addBefore(patternMethod, testMethod);
			}
			else
			{
				psiMethod = (PsiMethod) clazz.add(patternMethod);
			}
			JavaCodeStyleManager.getInstance(clazz.getProject()).shortenClassReferences(clazz);
			return psiMethod;
		}
		else if(inClass.getBody() == null)
		{
			return (PsiMethod) inClass.replace(patternMethod);
		}
		return inClass;
	}

	@Override
	public char getMnemonic()
	{
		return 'N';
	}

	@Override
	public FileTemplateDescriptor getTestClassFileTemplateDescriptor()
	{
		return new FileTemplateDescriptor("TestNG Test Class.java");
	}

	@Override
	public FileTemplateDescriptor getSetUpMethodFileTemplateDescriptor()
	{
		return new FileTemplateDescriptor("TestNG SetUp Method.java");
	}

	@Override
	public FileTemplateDescriptor getTearDownMethodFileTemplateDescriptor()
	{
		return new FileTemplateDescriptor("TestNG TearDown Method.java");
	}

	@Override
	@NotNull
	public FileTemplateDescriptor getTestMethodFileTemplateDescriptor()
	{
		return new FileTemplateDescriptor("TestNG Test Method.java");
	}

	@Nullable
	@Override
	public FileTemplateDescriptor getParametersMethodFileTemplateDescriptor()
	{
		return new FileTemplateDescriptor("TestNG Parameters Method.java");
	}

	@Override
	public boolean isTestMethod(PsiElement element, boolean checkAbstract)
	{
		return element instanceof PsiMethod && TestNGUtil.hasTest((PsiModifierListOwner) element);
	}

	@Override
	public boolean isMyConfigurationType(ConfigurationType type)
	{
		return type instanceof TestNGConfigurationType;
	}
}
