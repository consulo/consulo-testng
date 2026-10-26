/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.theoryinpractice.testng.configuration.browser;

import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserFactory;
import consulo.project.Project;
import consulo.testng.localize.TestNGLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;

import java.util.concurrent.CompletableFuture;

/**
 * @author Hani Suleiman
 */
public class SuiteBrowser
{
	private SuiteBrowser()
	{
	}

	public static FileChooserDescriptor createSuiteDescriptor()
	{
		return new FileChooserDescriptor(true, false, false, false, false, false)
				.withFileFilter(file -> "xml".equals(file.getExtension()))
				.withExtensionFilter("xml")
				.withTitle(TestNGLocalize.testngSuiteBrowserSelectSuite())
				.withDescription(TestNGLocalize.testngSuiteBrowserSelectXmlSuiteFile());
	}

	/**
	 * Chooses a suite file - completes on the UI thread, cancelled when nothing is chosen.
	 */
	@RequiredUIAccess
	public static CompletableFuture<VirtualFile> chooseSuite(Project project)
	{
		return FileChooserFactory.getInstance().newBuilder(createSuiteDescriptor()).project(project).showSingleAsVirtualFile();
	}
}
