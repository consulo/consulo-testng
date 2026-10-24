/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.theoryinpractice.testng.configuration;

import com.intellij.java.execution.JavaExecutionUtil;
import com.intellij.java.execution.configurations.JavaRunConfigurationModule;
import com.intellij.java.execution.configurations.RemoteConnectionCreator;
import com.intellij.java.execution.impl.JavaRunConfigurationExtensionManager;
import com.intellij.java.execution.impl.JavaTestConfigurationBase;
import com.intellij.java.execution.impl.RunConfigurationExtension;
import com.intellij.java.execution.impl.junit.JUnitUtil;
import com.intellij.java.execution.impl.junit.RefactoringListeners;
import com.intellij.java.execution.impl.junit2.info.MethodLocation;
import com.intellij.java.execution.impl.util.JavaParametersUtil;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiJavaPackage;
import com.intellij.java.language.psi.PsiMethod;
import com.theoryinpractice.testng.configuration.testDiscovery.TestNGTestDiscoveryRunnableState;
import com.theoryinpractice.testng.model.TestNGConsoleProperties;
import com.theoryinpractice.testng.model.TestNGTestObject;
import com.theoryinpractice.testng.model.TestType;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.action.Location;
import consulo.execution.action.PsiLocation;
import consulo.execution.action.RunConfigurationProducer;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.LegacyRunConfigurationConverter;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.fragment.EnvironmentFragmentState;
import consulo.execution.configuration.fragment.ProgramParametersFragmentState;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.WorkingDirectoryFragmentState;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.test.TestSearchScope;
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties;
import consulo.execution.util.ProgramParametersUtil;
import consulo.java.execution.configurations.fragment.JreFragmentState;
import consulo.java.execution.configurations.fragment.VmParametersFragmentState;
import consulo.language.editor.refactoring.event.RefactoringElementAdapter;
import consulo.language.editor.refactoring.event.RefactoringElementListener;
import consulo.language.editor.refactoring.event.UndoRefactoringElementListener;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiPackage;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class TestNGConfiguration extends JavaTestConfigurationBase
{
	static final String MODULE_WITH_DEPENDENCIES_SCOPE = "MODULE_WITH_DEPENDENCIES";

	private static final String LEGACY_PATTERNS = "patterns";
	private static final String LEGACY_PATTERN = "pattern";
	private static final String LEGACY_TEST_CLASS = "testClass";
	private static final String LEGACY_SEARCH_SCOPE = "TEST_SEARCH_SCOPE";

	@Nullable
	private String myChangeList;

	private final RefactoringListeners.Accessor<PsiJavaPackage> myPackage = new RefactoringListeners.Accessor<PsiJavaPackage>()
	{
		@Override
		public void setName(final String qualifiedName)
		{
			final boolean generatedName = isGeneratedName();
			getOptions().setPackageName(qualifiedName);
			if(generatedName)
			{
				setGeneratedName();
			}
		}

		@Nullable
		@Override
		public PsiJavaPackage getPsiElement()
		{
			final String qualifiedName = getOptions().getPackageName();
			return JavaPsiFacade.getInstance(getProject()).findPackage(qualifiedName);
		}

		@Override
		public void setPsiElement(final PsiJavaPackage psiPackage)
		{
			setName(psiPackage.getQualifiedName());
		}
	};

	private final RefactoringListeners.Accessor<PsiClass> myClass = new RefactoringListeners.Accessor<PsiClass>()
	{
		@Override
		public void setName(final String qualifiedName)
		{
			final boolean generatedName = isGeneratedName();
			getOptions().setMainClassName(qualifiedName);
			if(generatedName)
			{
				setGeneratedName();
			}
		}

		@Nullable
		@Override
		public PsiClass getPsiElement()
		{
			final String qualifiedName = getOptions().getMainClassName();
			return JavaPsiFacade.getInstance(getProject()).findClass(qualifiedName, GlobalSearchScope.allScope(getProject()));
		}

		@Override
		public void setPsiElement(final PsiClass psiClass)
		{
			setName(psiClass.getQualifiedName());
		}
	};

	public TestNGConfiguration(String s, Project project, ConfigurationFactory factory)
	{
		super(s, new JavaRunConfigurationModule(project, false), factory);
	}

	@Override
	protected Class<TestNGConfigurationOptions> getOptionsClass()
	{
		return TestNGConfigurationOptions.class;
	}

	@Override
	public TestNGConfigurationOptions getOptions()
	{
		return (TestNGConfigurationOptions) super.getOptions();
	}

	@Override
	protected void convertLegacyState(LegacyRunConfigurationConverter converter)
	{
		super.convertLegacyState(converter);

		TestNGConfigurationLegacyState old = converter.readXml(TestNGConfigurationLegacyState.class);
		Element element = converter.getElement();

		TestNGConfigurationOptions options = new TestNGConfigurationOptions();
		options.setTestObject(old.TEST_OBJECT);
		options.setSuiteName(old.SUITE_NAME);
		options.setPackageName(old.PACKAGE_NAME);
		options.setMainClassName(old.MAIN_CLASS_NAME);
		options.setMethodName(old.METHOD_NAME);
		options.setGroupName(old.GROUP_NAME);
		options.setOutputDirectory(old.OUTPUT_DIRECTORY);
		options.setPropertiesFile(old.PROPERTIES_FILE);
		options.setUseDefaultReporters(old.USE_DEFAULT_REPORTERS);
		options.setSearchScope(readLegacySearchScope(element));

		Map<String, String> properties = new LinkedHashMap<>();
		Element propertiesElement = element.getChild("properties");
		if(propertiesElement != null)
		{
			for(Element property : propertiesElement.getChildren("property"))
			{
				String name = property.getAttributeValue("name");
				if(name != null)
				{
					properties.put(name, StringUtil.notNullize(property.getAttributeValue("value")));
				}
			}
		}
		options.setProperties(properties);

		List<String> listeners = new ArrayList<>();
		Element listenersElement = element.getChild("listeners");
		if(listenersElement != null)
		{
			for(Element listener : listenersElement.getChildren("listener"))
			{
				String className = listener.getAttributeValue("class");
				if(className != null)
				{
					listeners.add(className);
				}
			}
		}
		options.setListeners(listeners);

		Set<String> patterns = new LinkedHashSet<>();
		Element patternsElement = element.getChild(LEGACY_PATTERNS);
		if(patternsElement != null)
		{
			for(Element pattern : patternsElement.getChildren(LEGACY_PATTERN))
			{
				String testClass = pattern.getAttributeValue(LEGACY_TEST_CLASS);
				if(testClass != null)
				{
					patterns.add(testClass);
				}
			}
		}
		options.setPatterns(new ArrayList<>(patterns));
		converter.setOptions(options);

		converter.addFragment(new ProgramParametersFragmentState(old.PARAMETERS));
		converter.addFragment(new WorkingDirectoryFragmentState(old.WORKING_DIRECTORY));
		converter.addFragment(new EnvironmentFragmentState(converter.readEnvs(), old.PASS_PARENT_ENVS));
		converter.addFragment(new VmParametersFragmentState(old.VM_PARAMETERS));
		if(old.ALTERNATIVE_JRE_PATH_ENABLED)
		{
			converter.addFragment(new JreFragmentState(old.ALTERNATIVE_JRE_PATH));
		}
	}

	private static String readLegacySearchScope(Element element)
	{
		for(Element option : element.getChildren("option"))
		{
			if(!LEGACY_SEARCH_SCOPE.equals(option.getAttributeValue("name")))
			{
				continue;
			}

			Element value = option.getChild("value");
			if(value == null)
			{
				return MODULE_WITH_DEPENDENCIES_SCOPE;
			}

			String name = value.getAttributeValue("defaultName");
			if("singleModule".equals(name))
			{
				return TestSearchScope.SINGLE_MODULE.toString();
			}
			if("moduleWithDependencies".equals(name))
			{
				return MODULE_WITH_DEPENDENCIES_SCOPE;
			}
			return TestSearchScope.WHOLE_PROJECT.toString();
		}
		return MODULE_WITH_DEPENDENCIES_SCOPE;
	}

	@Override
	public List<RunConfigurationFragment<?>> createFragments()
	{
		return List.of(
				new TestNGTestTargetFragment(),
				new TestNGOutputDirectoryFragment(),
				new TestNGPropertiesFileFragment(),
				new TestNGUseDefaultReportersFragment(),
				new TestNGParametersFragment(),
				new TestNGListenersFragment()
		);
	}

	@Nullable
	public RemoteConnectionCreator getRemoteConnectionCreator()
	{
		return null;
	}

	@Override
	public RunProfileState getState(final Executor executor, final ExecutionEnvironment env)
	{
		if(getOptions().getTestObject().equals(TestType.SOURCE.getType()) || getChangeList() != null)
		{
			return new TestNGTestDiscoveryRunnableState(env, this);
		}
		return new TestNGRunnableState(env, this);
	}

	@Override
	public Collection<Module> getValidModules()
	{
		//TODO add handling for package
		return JavaRunConfigurationModule.getModulesForClass(getProject(), getOptions().getMainClassName());
	}

	@Override
	public String suggestedName()
	{
		final TestNGTestObject testObject = TestNGTestObject.fromConfig(this);
		return testObject != null ? testObject.getGeneratedName() : null;
	}

	@Override
	public String getActionName()
	{
		final TestNGTestObject testObject = TestNGTestObject.fromConfig(this);
		return testObject != null ? testObject.getActionName() : null;
	}

	@Override
	public String getRunClass()
	{
		String testObject = getOptions().getTestObject();
		return !testObject.equals(TestType.CLASS.getType()) && !testObject.equals(TestType.METHOD.getType()) ? null : getOptions().getMainClassName();
	}

	@Override
	public String getPackage()
	{
		return !getOptions().getTestObject().equals(TestType.PACKAGE.getType()) ? null : getOptions().getPackageName();
	}

	public Set<String> getPatterns()
	{
		return new LinkedHashSet<>(getOptions().getPatterns());
	}

	public void setPatterns(Collection<String> patterns)
	{
		getOptions().setPatterns(new ArrayList<>(new LinkedHashSet<>(patterns)));
	}

	@Nullable
	public String getChangeList()
	{
		return myChangeList;
	}

	public void setChangeList(@Nullable String changeList)
	{
		myChangeList = changeList;
	}

	public void setTestSearchScope(TestSearchScope scope)
	{
		getOptions().setSearchScope(scope.toString());
	}

	@Override
	public void beClassConfiguration(PsiClass psiclass)
	{
		setModule(setMainClass(psiclass));
		getOptions().setTestObject(TestType.CLASS.getType());
		setGeneratedName();
	}

	@Override
	public boolean isConfiguredByElement(PsiElement element)
	{
		return TestNGTestObject.fromConfig(this).isConfiguredByElement(element);
	}

	@Override
	public String prepareParameterizedParameter(String paramSetName)
	{
		return TestNGConfigurationProducer.getInvocationNumber(paramSetName);
	}

	@Override
	public TestSearchScope getTestSearchScope()
	{
		String name = getOptions().getSearchScope();
		if(TestSearchScope.WHOLE_PROJECT.toString().equals(name))
		{
			return TestSearchScope.WHOLE_PROJECT;
		}
		if(TestSearchScope.SINGLE_MODULE.toString().equals(name))
		{
			return TestSearchScope.SINGLE_MODULE;
		}
		return TestSearchScope.MODULE_WITH_DEPENDENCIES;
	}

	public void setPackageConfiguration(Module module, PsiPackage pkg)
	{
		getOptions().setPackageName(pkg.getQualifiedName());
		setModule(module);
		getOptions().setTestObject(TestType.PACKAGE.getType());
		setGeneratedName();
	}

	@Override
	public void beMethodConfiguration(Location<PsiMethod> location)
	{
		setModule(setTestMethod(location));
		setGeneratedName();
	}

	@Deprecated
	public void setClassConfiguration(PsiClass psiclass)
	{
		beClassConfiguration(psiclass);
	}

	@Deprecated
	public void setMethodConfiguration(Location<PsiMethod> location)
	{
		beMethodConfiguration(location);
	}

	@Override
	public void bePatternConfiguration(List<PsiClass> classes, PsiMethod method)
	{
		TestNGConfigurationOptions options = getOptions();
		options.setTestObject(TestType.PATTERN.getType());
		final String suffix;
		if(method != null)
		{
			options.setMethodName(method.getName());
			suffix = "," + options.getMethodName();
		}
		else
		{
			suffix = "";
		}
		LinkedHashSet<String> patterns = new LinkedHashSet<>();
		for(PsiClass pattern : classes)
		{
			patterns.add(JavaExecutionUtil.getRuntimeQualifiedName(pattern) + suffix);
		}
		setPatterns(patterns);
		final Module module = RunConfigurationProducer.getInstance(AbstractTestNGPatternConfigurationProducer.class).findModule(this, getConfigurationModule().getModule(), patterns);
		if(module == null)
		{
			setTestSearchScope(TestSearchScope.WHOLE_PROJECT);
			setModule(null);
		}
		else
		{
			setModule(module);
		}
		setGeneratedName();
	}

	@Nullable
	private Module setMainClass(PsiClass psiClass)
	{
		TestNGConfigurationOptions options = getOptions();
		options.setMainClassName(JavaExecutionUtil.getRuntimeQualifiedName(psiClass));
		PsiJavaPackage psiPackage = JUnitUtil.getContainingPackage(psiClass);
		options.setPackageName(psiPackage == null ? "" : psiPackage.getQualifiedName());
		return JavaExecutionUtil.findModule(psiClass);
	}

	@Nullable
	private Module setTestMethod(Location<PsiMethod> location)
	{
		final PsiMethod method = location.getPsiElement();
		getOptions().setMethodName(method.getName());
		getOptions().setTestObject(TestType.METHOD.getType());
		return setMainClass(location instanceof MethodLocation methodLocation ? methodLocation.getContainingClass() : method.getContainingClass());
	}

	@Override
	public void checkConfiguration() throws RuntimeConfigurationException
	{
		final TestNGTestObject testObject = TestNGTestObject.fromConfig(this);
		testObject.checkConfiguration();
		JavaRunConfigurationExtensionManager.checkConfigurationIsValid(this);
		ProgramParametersUtil.checkWorkingDirectoryExist(this, getProject(), getConfigurationModule().getModule());
		JavaParametersUtil.checkAlternativeJRE(this);
		//TODO add various checks here
	}

	@Nullable
	@Override
	public RefactoringElementListener getRefactoringElementListener(final PsiElement element)
	{
		String testObject = getOptions().getTestObject();
		if(testObject.equals(TestType.PACKAGE.getType()))
		{
			if(!(element instanceof PsiPackage))
			{
				return null;
			}
			final RefactoringElementListener listener = RefactoringListeners.getListener((PsiJavaPackage) element, myPackage);
			return RunConfigurationExtension.wrapRefactoringElementListener(element, this, listener);
		}
		else if(testObject.equals(TestType.CLASS.getType()))
		{
			if(!(element instanceof PsiClass) && !(element instanceof PsiPackage))
			{
				return null;
			}
			final RefactoringElementListener listener = RefactoringListeners.getClassOrPackageListener(element, myClass);
			return RunConfigurationExtension.wrapRefactoringElementListener(element, this, listener);
		}
		else if(testObject.equals(TestType.METHOD.getType()))
		{
			if(!(element instanceof PsiMethod))
			{
				final RefactoringElementListener listener = RefactoringListeners.getClassOrPackageListener(element, myClass);
				return RunConfigurationExtension.wrapRefactoringElementListener(element, this, listener);
			}
			final PsiMethod method = (PsiMethod) element;
			if(!method.getName().equals(getOptions().getMethodName()))
			{
				return null;
			}
			if(!method.getContainingClass().equals(myClass.getPsiElement()))
			{
				return null;
			}
			class Listener extends RefactoringElementAdapter implements UndoRefactoringElementListener
			{
				@Override
				public void elementRenamedOrMoved(final PsiElement newElement)
				{
					setTestMethod(PsiLocation.fromPsiElement((PsiMethod) newElement));
				}

				@Override
				public void undoElementMovedOrRenamed(PsiElement newElement, String oldQualifiedName)
				{
					final int methodIdx = oldQualifiedName.indexOf("#") + 1;
					if(methodIdx <= 0 || methodIdx >= oldQualifiedName.length())
					{
						return;
					}
					getOptions().setMethodName(oldQualifiedName.substring(methodIdx));
				}
			}
			return RunConfigurationExtension.wrapRefactoringElementListener(element, this, new Listener());
		}
		return null;
	}

	@Override
	public boolean collectOutputFromProcessHandler()
	{
		return false;
	}

	@Override
	public SMTRunnerConsoleProperties createTestConsoleProperties(Executor executor)
	{
		return new TestNGConsoleProperties(this, executor);
	}

	@Override
	public String getFrameworkPrefix()
	{
		return "g";
	}

	@Nullable
	public Set<String> calculateGroupNames()
	{
		if(!TestType.GROUP.getType().equals(getOptions().getTestObject()))
		{
			return null;
		}

		Set<String> groups = StringUtil.split(getOptions().getGroupName(), ",").stream().map(String::trim).filter(StringUtil::isNotEmpty).collect(Collectors.toSet());
		return groups.isEmpty() ? null : groups;
	}

	public void beFromSourcePosition(PsiLocation<PsiMethod> position)
	{
		beMethodConfiguration(position);
		getOptions().setTestObject(TestType.SOURCE.getType());
	}
}
