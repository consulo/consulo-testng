/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.theoryinpractice.testng.configuration;

import com.theoryinpractice.testng.model.TestData;
import com.theoryinpractice.testng.model.TestNGTestObject;
import consulo.annotation.component.ExtensionImpl;
import consulo.component.extension.ExtensionInstance;
import consulo.execution.RunManager;
import consulo.execution.action.Location;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.configuration.ModuleBasedConfiguration;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.SimpleConfigurationType;
import consulo.java.language.module.extension.JavaModuleExtension;
import consulo.language.psi.PsiElement;
import consulo.module.Module;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.project.Project;
import consulo.testng.icon.TestNGIconGroup;
import consulo.testng.localize.TestNGLocalize;
import consulo.util.lang.Comparing;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

@ExtensionImpl
public class TestNGConfigurationType extends SimpleConfigurationType
{
	private static final Supplier<TestNGConfigurationType> INSTANCE = ExtensionInstance.from(ConfigurationType.class);

	@Nonnull
	public static TestNGConfigurationType getInstance()
	{
		return INSTANCE.get();
	}

	public TestNGConfigurationType()
	{
		super("TestNG", TestNGLocalize.testngConfigurationName(), TestNGLocalize.testngConfigurationDescription(), TestNGIconGroup.testng());
	}

	public boolean isConfigurationByLocation(RunConfiguration runConfiguration, Location location)
	{
		TestNGConfiguration config = (TestNGConfiguration) runConfiguration;
		TestData testobject = config.getPersistantData();
		if(testobject == null)
		{
			return false;
		}
		else
		{
			final PsiElement element = location.getPsiElement();
			final TestNGTestObject testNGTestObject = TestNGTestObject.fromConfig(config);
			if(testNGTestObject != null && testNGTestObject.isConfiguredByElement(element))
			{
				final Module configurationModule = config.getConfigurationModule().getModule();
				if(Comparing.equal(location.getModule(), configurationModule))
				{
					return true;
				}

				final Module predefinedModule = ((TestNGConfiguration) RunManager.getInstance(location.getProject()).getConfigurationTemplate(this).getConfiguration()).getConfigurationModule()
						.getModule();
				return Comparing.equal(predefinedModule, configurationModule);

			}
			else
			{
				return false;
			}
		}
	}

	@NotNull
	@Override
	public RunConfiguration createTemplateConfiguration(Project project)
	{
		return new TestNGConfiguration("", project, this);
	}

	@Override
	public boolean isApplicable(@NotNull Project project)
	{
		return ModuleExtensionHelper.getInstance(project).hasModuleExtension(JavaModuleExtension.class);
	}

	@Override
	public void onNewConfigurationCreated(@NotNull RunConfiguration configuration)
	{
		((ModuleBasedConfiguration) configuration).onNewConfigurationCreated();
	}
}
