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

import com.intellij.java.execution.impl.ui.BaseConfigurationModuleSelector;
import com.intellij.java.language.psi.PsiClass;
import com.theoryinpractice.testng.model.TestClassFilter;
import com.theoryinpractice.testng.util.TestNGUtil;
import consulo.execution.ui.awt.BrowseModuleValueActionListener;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.project.Project;
import consulo.testng.localize.TestNGLocalize;
import consulo.ui.ex.awt.Messages;
import org.jspecify.annotations.Nullable;

import javax.swing.*;

/**
 * @author Hani Suleiman
 */
public class GroupBrowser extends BrowseModuleValueActionListener {
    private final BaseConfigurationModuleSelector moduleSelector;

    public GroupBrowser(Project project, BaseConfigurationModuleSelector moduleSelector) {
        super(project);
        this.moduleSelector = moduleSelector;
    }

    @Override
    protected @Nullable String showDialog() {
        return chooseGroup(getField());
    }

    public @Nullable String chooseGroup(JComponent parent) {
        TestClassFilter filter;
        Module module = moduleSelector.getModule();
        if (module == null) {
            filter = new TestClassFilter(GlobalSearchScope.projectScope(getProject()), getProject(), false);
        }
        else {
            filter = new TestClassFilter(GlobalSearchScope.moduleScope(module), getProject(), false);
        }
        PsiClass[] classes = TestNGUtil.getAllTestClasses(filter, true);
        if (classes == null || classes.length == 0) {
            Messages.showMessageDialog(
                parent,
                TestNGLocalize.testngGroupBrowserNoTestsFoundInProject().get(),
                TestNGLocalize.testngGroupBrowserCannotBrowseGroups().get(),
                Messages.getInformationIcon()
            );
            return null;
        }
        else {
            return GroupList.showDialog(classes, parent);
        }
    }
}
