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

package com.theoryinpractice.testng.configuration;

import javax.swing.Icon;

import org.consulo.java.module.extension.JavaModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.module.extension.ModuleExtensionHelper;
import com.intellij.execution.Location;
import com.intellij.execution.RunManagerEx;
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
import icons.TestngIcons;

/*
 * Created by IntelliJ IDEA.
 * User: amrk
 * Date: Jul 2, 2005
 * Time: 12:10:47 AM
 */
public class TestNGConfigurationType implements ConfigurationType
{
	private final ConfigurationFactory myFactory;

	public TestNGConfigurationType()
	{

		myFactory = new ConfigurationFactoryEx(this)
		{
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
			if(testobject.isConfiguredByElement(element))
			{
				final Module configurationModule = config.getConfigurationModule().getModule();
				if(Comparing.equal(location.getModule(), configurationModule))
				{
					return true;
				}

				final Module predefinedModule = ((TestNGConfiguration) RunManagerEx.getInstanceEx(location.getProject()).getConfigurationTemplate(myFactory).getConfiguration()).getConfigurationModule().getModule();
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
	public Icon getIcon()
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
