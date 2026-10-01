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

import consulo.execution.ui.awt.BrowseModuleValueActionListener;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.IdeaFileChooser;
import consulo.project.Project;
import consulo.testng.localize.TestNGLocalize;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author Hani Suleiman
 */
public class SuiteBrowser extends BrowseModuleValueActionListener
{
	public SuiteBrowser(Project project)
	{
		super(project);
	}

	public static FileChooserDescriptor createSuiteDescriptor()
	{
		return new FileChooserDescriptor(true, false, false, false, false, false)
				.withFileFilter(file -> "xml".equals(file.getExtension()))
				.withExtensionFilter("xml")
				.withTitle(TestNGLocalize.testngSuiteBrowserSelectSuite())
				.withDescription(TestNGLocalize.testngSuiteBrowserSelectXmlSuiteFile());
	}

	@Override
	public String showDialog()
	{
		VirtualFile file = IdeaFileChooser.chooseFile(createSuiteDescriptor(), getProject(), null);
		return file != null ? file.getPath() : null;
	}
}
