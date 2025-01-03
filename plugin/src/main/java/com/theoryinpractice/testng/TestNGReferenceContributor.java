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
 * Date: 11/11/2006
 * Time: 16:15:10
 */
package com.theoryinpractice.testng;

import com.intellij.java.language.JavaLanguage;
import com.intellij.java.language.codeInsight.AnnotationUtil;
import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.PsiUtil;
import com.theoryinpractice.testng.inspection.DependsOnGroupsInspection;
import com.theoryinpractice.testng.inspection.DependsOnGroupsInspectionState;
import com.theoryinpractice.testng.util.TestNGUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.completion.lookup.LookupValueFactory;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.pattern.FilterPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.*;
import consulo.language.psi.filter.ElementFilter;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.DataProvider;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@ExtensionImpl
public class TestNGReferenceContributor extends PsiReferenceContributor
{
	private static PsiElementPattern.Capture<PsiLiteralExpression> getElementPattern(String annotation)
	{
		return PlatformPatterns.psiElement(PsiLiteralExpression.class).and(new FilterPattern(new TestAnnotationFilter(annotation)));
	}

	@Override
	public void registerReferenceProviders(PsiReferenceRegistrar registrar)
	{
		registrar.registerReferenceProvider(getElementPattern("dependsOnMethods"), new PsiReferenceProvider()
		{
			@Override
			@NotNull
			public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull final ProcessingContext context)
			{
				return new MethodReference[]{new MethodReference((PsiLiteralExpression) element)};
			}
		});

		registrar.registerReferenceProvider(getElementPattern("dataProvider"), new PsiReferenceProvider()
		{
			@Override
			@NotNull
			public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull final ProcessingContext context)
			{
				return new DataProviderReference[]{new DataProviderReference((PsiLiteralExpression) element)};
			}
		});
		registrar.registerReferenceProvider(getElementPattern("groups"), new PsiReferenceProvider()
		{
			@Override
			@NotNull
			public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull final ProcessingContext context)
			{
				return new GroupReference[]{new GroupReference(element.getProject(), (PsiLiteralExpression) element)};
			}
		});
		registrar.registerReferenceProvider(getElementPattern("dependsOnGroups"), new PsiReferenceProvider()
		{
			@Override
			@NotNull
			public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull final ProcessingContext context)
			{
				return new GroupReference[]{new GroupReference(element.getProject(), (PsiLiteralExpression) element)};
			}
		});
	}

	private static class DataProviderReference extends PsiReferenceBase<PsiLiteralExpression>
	{

		public DataProviderReference(PsiLiteralExpression element)
		{
			super(element, false);
		}

		@Override
		@Nullable
		public PsiElement resolve()
		{
			final PsiClass cls = getProviderClass(PsiUtil.getTopLevelClass(getElement()));
			if(cls != null)
			{
				PsiMethod[] methods = cls.getAllMethods();
				@NonNls String val = getValue();
				for(PsiMethod method : methods)
				{
					PsiAnnotation dataProviderAnnotation = AnnotationUtil.findAnnotation(method, DataProvider.class.getName());
					if(dataProviderAnnotation != null)
					{
						PsiNameValuePair[] values = dataProviderAnnotation.getParameterList().getAttributes();
						for(PsiNameValuePair value : values)
						{
							if("name".equals(value.getName()))
							{
								final PsiAnnotationMemberValue dataProviderMethodName = value.getValue();
								if(dataProviderMethodName != null && val.equals(StringUtil.unquoteString(dataProviderMethodName.getText())))
								{
									return method;
								}
							}
						}
						if(val.equals(method.getName()))
						{
							return method;
						}
					}
				}
			}
			return null;
		}

		@Override
		@NotNull
		public Object[] getVariants()
		{
			final List<Object> list = new ArrayList<Object>();
			final PsiClass topLevelClass = PsiUtil.getTopLevelClass(getElement());
			final PsiClass cls = getProviderClass(topLevelClass);
			final boolean needToBeStatic = cls != topLevelClass;
			if(cls != null)
			{
				final PsiMethod current = PsiTreeUtil.getParentOfType(getElement(), PsiMethod.class);
				final PsiMethod[] methods = cls.getAllMethods();
				for(PsiMethod method : methods)
				{
					if(current != null && method.getName().equals(current.getName()))
					{
						continue;
					}
					if(needToBeStatic)
					{
						if(!method.hasModifierProperty(PsiModifier.STATIC))
						{
							continue;
						}
					}
					else
					{
						if(cls != method.getContainingClass() && method.hasModifierProperty(PsiModifier.PRIVATE))
						{
							continue;
						}
					}
					final PsiAnnotation dataProviderAnnotation = AnnotationUtil.findAnnotation(method, DataProvider.class.getName());
					if(dataProviderAnnotation != null)
					{
						boolean nameFoundInAttributes = false;
						PsiNameValuePair[] values = dataProviderAnnotation.getParameterList().getAttributes();
						for(PsiNameValuePair value : values)
						{
							if("name".equals(value.getName()))
							{
								final PsiAnnotationMemberValue memberValue = value.getValue();
								if(memberValue != null)
								{
									list.add(LookupElementBuilder.create(StringUtil.unquoteString(memberValue.getText())));
									nameFoundInAttributes = true;
									break;
								}
							}
						}
						if(!nameFoundInAttributes)
						{
							list.add(LookupElementBuilder.create(method.getName()));
						}
					}
				}
			}
			return list.toArray();
		}

		private PsiClass getProviderClass(final PsiClass topLevelClass)
		{
			final PsiAnnotationParameterList parameterList = PsiTreeUtil.getParentOfType(getElement(), PsiAnnotationParameterList.class);
			if(parameterList != null)
			{
				for(PsiNameValuePair nameValuePair : parameterList.getAttributes())
				{
					if(Comparing.strEqual(nameValuePair.getName(), "dataProviderClass"))
					{
						final PsiAnnotationMemberValue value = nameValuePair.getValue();
						if(value instanceof PsiClassObjectAccessExpression)
						{
							final PsiTypeElement operand = ((PsiClassObjectAccessExpression) value).getOperand();
							final PsiClass psiClass = PsiUtil.resolveClassInType(operand.getType());
							if(psiClass != null)
							{
								return psiClass;
							}
						}
						break;
					}
				}
			}
			return topLevelClass;
		}
	}

	private static class MethodReference extends PsiReferenceBase<PsiLiteralExpression>
	{

		public MethodReference(PsiLiteralExpression element)
		{
			super(element, false);
		}

		@Override
		@Nullable
		public PsiElement resolve()
		{
			@NonNls String val = getValue();
			final String methodName = StringUtil.getShortName(val);
			PsiClass cls = getDependsClass(val);
			if(cls != null)
			{
				PsiMethod[] methods = cls.findMethodsByName(methodName, true);
				for(PsiMethod method : methods)
				{
					if(TestNGUtil.hasTest(method))
					{
						return method;
					}
				}
			}
			return null;
		}

		@Nullable
		private PsiClass getDependsClass(String val)
		{
			final String className = StringUtil.getPackageName(val);
			final PsiLiteralExpression element = getElement();
			return StringUtil.isEmpty(className) ? PsiUtil.getTopLevelClass(element)
					: JavaPsiFacade.getInstance(element.getProject()).findClass(className, element.getResolveScope());
		}

		@Override
		@NotNull
		public Object[] getVariants()
		{
			List<Object> list = new ArrayList<Object>();
			@NonNls String val = getValue();
			int hackIndex = val.indexOf(CompletionUtilCore.DUMMY_IDENTIFIER);
			if(hackIndex > -1)
			{
				val = val.substring(0, hackIndex);
			}
			final String className = StringUtil.getPackageName(val);
			PsiClass cls = getDependsClass(val);
			if(cls != null)
			{
				PsiMethod current = PsiTreeUtil.getParentOfType(getElement(), PsiMethod.class);
				PsiMethod[] methods = cls.getMethods();
				for(PsiMethod method : methods)
				{
					if(current != null && method.getName().equals(current.getName()))
					{
						continue;
					}
					if(TestNGUtil.hasTest(method) || TestNGUtil.hasConfig(method))
					{
						list.add(LookupValueFactory.createLookupValue(StringUtil.isEmpty(className) ? method.getName()
								: StringUtil.getQualifiedName(cls.getQualifiedName(), method.getName()), null));
					}
				}
			}
			return list.toArray();
		}
	}

	private static class GroupReference extends PsiReferenceBase<PsiLiteralExpression>
	{
		private final Project myProject;

		public GroupReference(Project project, PsiLiteralExpression element)
		{
			super(element, false);
			myProject = project;
		}

		@Override
		@Nullable
		public PsiElement resolve()
		{
			return null;
		}

		@Override
		@NotNull
		public Object[] getVariants()
		{
			List<Object> list = new ArrayList<Object>();

			InspectionProfile inspectionProfile = InspectionProjectProfileManager.getInstance(myProject).getInspectionProfile();
			DependsOnGroupsInspectionState inspection = (DependsOnGroupsInspectionState) inspectionProfile.getToolState(DependsOnGroupsInspection.SHORT_NAME, myElement);

			for(String groupName : inspection.groups)
			{
				list.add(LookupValueFactory.createLookupValue(groupName, null));
			}

			if(!list.isEmpty())
			{
				return list.toArray();
			}
			return ArrayUtil.EMPTY_OBJECT_ARRAY;
		}
	}

	private static class TestAnnotationFilter implements ElementFilter
	{

		private final String myParameterName;

		public TestAnnotationFilter(@NotNull @NonNls String parameterName)
		{
			myParameterName = parameterName;
		}

		@Override
		public boolean isAcceptable(Object element, PsiElement context)
		{
			PsiNameValuePair pair = PsiTreeUtil.getParentOfType(context, PsiNameValuePair.class);
			if(null == pair)
			{
				return false;
			}
			if(!myParameterName.equals(pair.getName()))
			{
				return false;
			}
			PsiAnnotation annotation = PsiTreeUtil.getParentOfType(pair, PsiAnnotation.class);
			if(annotation == null)
			{
				return false;
			}
			if(!TestNGUtil.isTestNGAnnotation(annotation))
			{
				return false;
			}
			return true;
		}

		@Override
		public boolean isClassAcceptable(Class hintClass)
		{
			return PsiLiteralExpression.class.isAssignableFrom(hintClass);
		}
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return JavaLanguage.INSTANCE;
	}
}
