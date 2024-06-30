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
package com.theoryinpractice.testng.configuration.browser;

import com.intellij.java.language.impl.ui.PackageChooser;
import com.intellij.java.language.impl.ui.PackageChooserFactory;
import com.intellij.java.language.psi.PsiJavaPackage;
import consulo.execution.ui.awt.BrowseModuleValueActionListener;
import consulo.project.Project;

import java.util.List;

/**
 * @author Hani Suleiman
 */
public class PackageBrowser extends BrowseModuleValueActionListener
{
	public PackageBrowser(Project project)
	{
		super(project);
	}

	@Override
	protected String showDialog()
	{
		PackageChooser packageChooser = getProject().getInstance(PackageChooserFactory.class).create();

		List<PsiJavaPackage> packages = packageChooser.showAndSelect();
		PsiJavaPackage psiPackage = packages == null || packages.isEmpty() ? null : packages.getFirst();
		String packageName = psiPackage == null ? null : psiPackage.getQualifiedName();
		return packageName;
	}
}
