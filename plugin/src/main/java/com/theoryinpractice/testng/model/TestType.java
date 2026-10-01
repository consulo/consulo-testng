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

package com.theoryinpractice.testng.model;

import consulo.localize.LocalizeValue;
import consulo.testng.localize.TestNGLocalize;

public enum TestType
{
	PACKAGE("PACKAGE", TestNGLocalize.labelAllInPackageTestType(), 0),
	CLASS("CLASS", TestNGLocalize.labelClassTestType(), 1),
	METHOD("METHOD", TestNGLocalize.labelMethodTestType(), 2),
	GROUP("GROUP", TestNGLocalize.labelGroupTestType(), 3),
	SUITE("SUITE", TestNGLocalize.labelSuiteTestType(), 4),
	PATTERN("PATTERN", TestNGLocalize.labelPatternTestType(), 5),
	SOURCE("SOURCE", TestNGLocalize.labelSourceLocationTestType(), 6);

	public final String type;
	private final LocalizeValue presentableName;
	public final int value;

	TestType(String type, LocalizeValue presentableName, int value)
	{
		this.type = type;
		this.presentableName = presentableName;
		this.value = value;
	}

	public String getType()
	{
		return type;
	}

	public int getValue()
	{
		return value;
	}

	public LocalizeValue getPresentableName()
	{
		return presentableName;
	}
}