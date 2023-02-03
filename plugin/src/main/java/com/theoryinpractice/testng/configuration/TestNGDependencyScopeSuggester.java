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
package com.theoryinpractice.testng.configuration;

import com.intellij.java.impl.openapi.roots.libraries.LibrariesHelper;
import consulo.annotation.component.ExtensionImpl;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.library.Library;
import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.module.content.layer.orderEntry.LibraryDependencyScopeSuggester;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * @author nik
 */
@ExtensionImpl
public class TestNGDependencyScopeSuggester extends LibraryDependencyScopeSuggester
{
	@Nullable
	@Override
	public DependencyScope getDefaultDependencyScope(@NotNull Library library)
	{
		VirtualFile[] files = library.getFiles(BinariesOrderRootType.getInstance());
		if(files.length == 1 && LibrariesHelper.getInstance().isClassAvailable(Arrays.stream(files).map(VirtualFile::getUrl).toArray(String[]::new), "org.testng.annotations.Test"))
		{
			return DependencyScope.TEST;
		}
		return null;
	}
}
