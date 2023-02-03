/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.java.execution.impl.testframework.AbstractInClassConfigurationProducer;
import consulo.annotation.component.ExtensionImpl;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.ConfigurationFromContext;
import consulo.execution.configuration.ConfigurationType;
import consulo.language.psi.PsiElement;
import consulo.util.lang.ref.Ref;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

@ExtensionImpl
public class TestNGInClassConfigurationProducer extends TestNGConfigurationProducer
{
	private TestNGInClassConfigurationProducerDelegate myDelegate = new TestNGInClassConfigurationProducerDelegate(TestNGConfigurationType.getInstance());

	@Inject
	protected TestNGInClassConfigurationProducer()
	{
		super(TestNGConfigurationType.getInstance());
	}

	@Override
	public void onFirstRun(@NotNull ConfigurationFromContext configuration, @NotNull ConfigurationContext fromContext, @NotNull Runnable performRunnable)
	{
		myDelegate.onFirstRun(configuration, fromContext, performRunnable);
	}

	@Override
	protected boolean setupConfigurationFromContext(TestNGConfiguration configuration, ConfigurationContext context, Ref<PsiElement> sourceElement)
	{
		return myDelegate.setupConfigurationFromContext(configuration, context, sourceElement);
	}

	private static class TestNGInClassConfigurationProducerDelegate extends AbstractInClassConfigurationProducer<TestNGConfiguration>
	{
		protected TestNGInClassConfigurationProducerDelegate(ConfigurationType configurationType)
		{
			super(configurationType);
		}

		@Override
		protected boolean setupConfigurationFromContext(TestNGConfiguration configuration, ConfigurationContext context, Ref<PsiElement> sourceElement)
		{
			return super.setupConfigurationFromContext(configuration, context, sourceElement);
		}
	}
}
