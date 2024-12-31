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

import com.beust.jcommander.JCommander;
import com.intellij.java.execution.impl.JavaTestFrameworkRunnableState;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.theoryinpractice.testng.model.TestData;
import consulo.container.plugin.PluginManager;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.test.TestSearchScope;
import consulo.java.execution.configurations.OwnJavaParameters;
import consulo.language.util.ModuleUtilCore;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilder;
import consulo.process.cmd.ParametersList;
import consulo.util.io.ClassPathUtil;
import consulo.util.io.NetUtil;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.testng.CommandLineArgs;

import jakarta.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class TestNGRunnableState extends JavaTestFrameworkRunnableState<TestNGConfiguration>
{
	private static final Logger LOG = Logger.getInstance("TestNG Runner");
	private static final String TESTNG_TEST_FRAMEWORK_NAME = "TestNG";
	private final TestNGConfiguration config;
	private int port;

	public TestNGRunnableState(ExecutionEnvironment environment, TestNGConfiguration config)
	{
		super(environment);
		this.config = config;
		//TODO need to narrow this down a bit
		//setModulesToCompile(ModuleManager.getInstance(config.getProject()).getModules());
	}

	@Override
	protected void buildProcessHandler(@Nonnull ProcessHandlerBuilder builder) throws ExecutionException
	{
		super.buildProcessHandler(builder);

		builder.colored().killable();
	}

	@Override
	protected void setupProcessHandler(@Nonnull ProcessHandler handler)
	{
		super.setupProcessHandler(handler);

		createSearchingForTestsTask().attachTaskToProcess(handler);
	}

	@NotNull
	@Override
	protected ProcessHandler createHandler(Executor executor) throws ExecutionException
	{
		appendForkInfo(executor);
		return startProcess();
	}

	@NotNull
	@Override
	protected String getFrameworkName()
	{
		return TESTNG_TEST_FRAMEWORK_NAME;
	}

	@Override
	protected boolean configureByModule(Module module)
	{
		return module != null && getConfiguration().getPersistantData().getScope() != TestSearchScope.WHOLE_PROJECT;
	}

	@Override
	protected void configureRTClasspath(OwnJavaParameters javaParameters)
	{
		File pluginPath = PluginManager.getPluginPath(TestNGRunnableState.class);

		javaParameters.getClassPath().add(new File(pluginPath, "testng-rt.jar"));
		javaParameters.getClassPath().addTail(ClassPathUtil.getJarPathForClass(JCommander.class));
	}

	@Override
	protected OwnJavaParameters createJavaParameters() throws ExecutionException
	{
		final OwnJavaParameters javaParameters = super.createJavaParameters();
		javaParameters.setMainClass("consulo.testng.rt.RemoteTestNGStarter");

		try
		{
			port = NetUtil.findAvailableSocketPort();
		}
		catch(IOException e)
		{
			throw new ExecutionException("Unable to bind to port " + port, e);
		}

		final TestData data = getConfiguration().getPersistantData();

		if(data.getOutputDirectory() != null && !data.getOutputDirectory().isEmpty())
		{
			javaParameters.getProgramParametersList().add(CommandLineArgs.OUTPUT_DIRECTORY, data.getOutputDirectory());
		}

		javaParameters.getProgramParametersList().add(CommandLineArgs.USE_DEFAULT_LISTENERS, String.valueOf(data.USE_DEFAULT_REPORTERS));

		@NonNls final StringBuilder buf = new StringBuilder();
		if(data.TEST_LISTENERS != null && !data.TEST_LISTENERS.isEmpty())
		{
			buf.append(StringUtil.join(data.TEST_LISTENERS, ";"));
		}
//		collectListeners(javaParameters, buf, IDEATestNGListener.EP_NAME.getName(), ";");
//		if(buf.length() > 0)
//		{
//			javaParameters.getProgramParametersList().add(CommandLineArgs.LISTENER, buf.toString());
//		}

		createServerSocket(javaParameters);
		createTempFiles(javaParameters);
		return javaParameters;
	}

	@Override
	protected List<String> getNamedParams(String parameters)
	{
		try
		{
			Integer.parseInt(parameters);
			return super.getNamedParams(parameters);
		}
		catch(NumberFormatException e)
		{
			return Arrays.asList(parameters.split(" "));
		}
	}

	@NotNull
	@Override
	protected String getForkMode()
	{
		return "none";
	}

	public SearchingForTestsTask createSearchingForTestsTask()
	{
		return new SearchingForTestsTask(myServerSocket, config, myTempFile)
		{
			@Override
			protected void onFound()
			{
				super.onFound();
				writeClassesPerModule(myClasses);
			}
		};
	}

	protected void writeClassesPerModule(Map<PsiClass, Map<PsiMethod, List<String>>> classes)
	{
		if(forkPerModule())
		{
			final Map<Module, List<String>> perModule = new TreeMap<>((o1, o2) -> StringUtil.compare(o1.getName(), o2.getName(), true));

			for(final PsiClass psiClass : classes.keySet())
			{
				final Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
				if(module != null)
				{
					List<String> list = perModule.get(module);
					if(list == null)
					{
						list = new ArrayList<>();
						perModule.put(module, list);
					}
					list.add(psiClass.getQualifiedName());
				}
			}

			try
			{
				writeClassesPerModule(getConfiguration().getPackage(), getJavaParameters(), perModule);
			}
			catch(Exception e)
			{
				LOG.error(e);
			}
		}
	}

	@NotNull
	protected String getFrameworkId()
	{
		return "testng";
	}

	protected void passTempFile(ParametersList parametersList, String tempFilePath)
	{
		parametersList.add("-temp", tempFilePath);
	}

	@NotNull
	public TestNGConfiguration getConfiguration()
	{
		return config;
	}

	@Override
	protected TestSearchScope getScope()
	{
		return getConfiguration().getPersistantData().getScope();
	}

	protected void passForkMode(String forkMode, File tempFile, OwnJavaParameters parameters) throws ExecutionException
	{
		final ParametersList parametersList = parameters.getProgramParametersList();
		final List<String> params = parametersList.getParameters();
		int paramIdx = params.size() - 1;
		for(int i = 0; i < params.size(); i++)
		{
			String param = params.get(i);
			if("-temp".equals(param))
			{
				paramIdx = i;
				break;
			}
		}
		parametersList.addAt(paramIdx, "@@@" + tempFile.getAbsolutePath());
		if(getForkSocket() != null)
		{
			parametersList.addAt(paramIdx, "-debugSocket" + getForkSocket().getLocalPort());
		}
	}
}
