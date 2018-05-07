/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.theoryinpractice.testng.configuration;

import org.jetbrains.annotations.NotNull;
import com.intellij.execution.Location;
import com.intellij.execution.RunManager;
import com.intellij.execution.configuration.ConfigurationFactoryEx;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiElement;
import com.theoryinpractice.testng.model.TestData;
import com.theoryinpractice.testng.model.TestNGTestObject;
import consulo.java.module.extension.JavaModuleExtension;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.ui.image.Image;
import icons.TestngIcons;

public class TestNGConfigurationType implements ConfigurationType
{
	private final ConfigurationFactory myFactory;

	public TestNGConfigurationType()
	{
		myFactory = new ConfigurationFactoryEx(this)
		{
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
		};
	}

	public static TestNGConfigurationType getInstance()
	{
		return ConfigurationTypeUtil.findConfigurationType(TestNGConfigurationType.class);
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

				final Module predefinedModule = ((TestNGConfiguration) RunManager.getInstance(location.getProject()).getConfigurationTemplate(myFactory).getConfiguration()).getConfigurationModule().getModule();
				return Comparing.equal(predefinedModule, configurationModule);

			}
			else
			{
				return false;
			}
		}
	}

	@Override
	public String getDisplayName()
	{
		return "TestNG";
	}

	@Override
	public String getConfigurationTypeDescription()
	{
		return "TestNG Configuration";
	}

	@Override
	public Image getIcon()
	{
		return TestngIcons.TestNG;
	}

	@Override
	public ConfigurationFactory[] getConfigurationFactories()
	{
		return new ConfigurationFactory[]{myFactory};
	}

	@Override
	@NotNull
	public String getId()
	{
		return "TestNG";
	}
}
