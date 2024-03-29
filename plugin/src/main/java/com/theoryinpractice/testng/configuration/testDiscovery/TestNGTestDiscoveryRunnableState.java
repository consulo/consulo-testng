/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.theoryinpractice.testng.configuration.testDiscovery;

import com.intellij.java.execution.impl.testDiscovery.TestDiscoverySearchHelper;
import com.theoryinpractice.testng.configuration.SearchingForTestsTask;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.configuration.TestNGRunnableState;
import com.theoryinpractice.testng.model.TestData;
import com.theoryinpractice.testng.model.TestNGTestPattern;
import com.theoryinpractice.testng.model.TestType;
import consulo.execution.CantRunException;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.test.TestSearchScope;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.lang.Pair;

import java.util.Set;

public class TestNGTestDiscoveryRunnableState extends TestNGRunnableState
{

	public TestNGTestDiscoveryRunnableState(ExecutionEnvironment environment, TestNGConfiguration configuration)
	{
		super(environment, configuration);
	}

	@Override
	protected TestSearchScope getScope()
	{
		return TestSearchScope.MODULE_WITH_DEPENDENCIES;
	}

	@Override
	protected boolean forkPerModule()
	{
		return getConfiguration().getConfigurationModule().getModule() == null;
	}

	@Override
	public SearchingForTestsTask createSearchingForTestsTask()
	{
		return new SearchingForTestsTask(myServerSocket, getConfiguration(), myTempFile)
		{
			@Override
			protected void search() throws CantRunException
			{
				myClasses.clear();
				final TestData data = getConfiguration().getPersistantData();
				final Pair<String, String> position = data.TEST_OBJECT.equals(TestType.SOURCE.getType()) ? Pair.create(data.getMainClassName(), data.getMethodName()) : null;
				final Set<String> patterns = TestDiscoverySearchHelper.search((Project) getProject(), position, data.getChangeList(), getConfiguration().getFrameworkPrefix());
				final Module module = getConfiguration().getConfigurationModule().getModule();
				final GlobalSearchScope searchScope = module != null ? GlobalSearchScope.moduleWithDependenciesScope(module) : GlobalSearchScope.projectScope((Project) getProject());
				TestNGTestPattern.fillTestObjects(myClasses, patterns, TestSearchScope.MODULE_WITH_DEPENDENCIES, getConfiguration(), searchScope);
			}

			@Override
			protected void onFound()
			{
				super.onFound();
				writeClassesPerModule(myClasses);
			}
		};
	}
}
