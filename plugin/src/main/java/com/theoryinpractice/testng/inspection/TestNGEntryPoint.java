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

/*
 * User: anna
 * Date: 14-May-2007
 */
package com.theoryinpractice.testng.inspection;

import com.intellij.java.analysis.codeInspection.ex.EntryPoint;
import com.intellij.java.language.psi.PsiModifierListOwner;
import com.theoryinpractice.testng.util.TestNGUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.psi.PsiElement;
import consulo.util.xml.serializer.DefaultJDOMExternalizer;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ExtensionImpl
public class TestNGEntryPoint extends EntryPoint
{
	public boolean ADD_TESTNG_TO_ENTRIES = true;

	public boolean isSelected()
	{
		return ADD_TESTNG_TO_ENTRIES;
	}

	public void setSelected(boolean selected)
	{
		ADD_TESTNG_TO_ENTRIES = selected;
	}

	@NotNull
	public String getDisplayName()
	{
		return "Automatically add all TestNG classes/methods/etc. to entry points";
	}

	public boolean isEntryPoint(RefElement refElement, PsiElement psiElement)
	{
		return isEntryPoint(psiElement);
	}

	@Override
	public boolean isEntryPoint(PsiElement psiElement)
	{
		if(ADD_TESTNG_TO_ENTRIES)
		{
			if(psiElement instanceof PsiModifierListOwner)
			{
				if(TestNGUtil.hasTest((PsiModifierListOwner) psiElement, false, false, TestNGUtil.hasDocTagsSupport))
				{
					return true;
				}
				return TestNGUtil.hasConfig((PsiModifierListOwner) psiElement);
			}
		}
		return false;
	}

	public void readExternal(Element element) throws InvalidDataException
	{
		DefaultJDOMExternalizer.readExternal(this, element);
	}

	public void writeExternal(Element element) throws WriteExternalException
	{
		if(!ADD_TESTNG_TO_ENTRIES)
		{
			DefaultJDOMExternalizer.writeExternal(this, element);
		}
	}

	@Nullable
	public String[] getIgnoreAnnotations()
	{
		return TestNGUtil.CONFIG_ANNOTATIONS_FQN;
	}
}