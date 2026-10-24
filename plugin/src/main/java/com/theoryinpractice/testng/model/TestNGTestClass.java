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
package com.theoryinpractice.testng.model;

import com.intellij.java.execution.JavaExecutionUtil;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.psi.PsiModifierListOwner;
import com.intellij.java.language.psi.util.ClassUtil;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.configuration.TestNGConfigurationOptions;
import consulo.application.ReadAction;
import consulo.execution.CantRunException;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.test.SourceScope;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.util.lang.Comparing;

import java.util.List;
import java.util.Map;

public class TestNGTestClass extends TestNGTestObject {
    public TestNGTestClass(TestNGConfiguration config) {
        super(config);
    }

    @Override
    public void fillTestObjects(Map<PsiClass, Map<PsiMethod, List<String>>> classes) throws CantRunException {
        TestNGConfigurationOptions data = myConfig.getOptions();
        //it's a class
        PsiClass psiClass = ReadAction.compute(() -> ClassUtil.findPsiClass(
            PsiManager.getInstance(myConfig.getProject()),
            data.getMainClassName().replace('/', '.'),
            null,
            true,
            getSearchScope
                ()
        ));
        if (psiClass == null) {
            throw new CantRunException(LocalizeValue.localizeTODO("No tests found in the class \"" + data.getMainClassName() + '\"'));
        }
        if (null == ReadAction.compute(() -> psiClass.getQualifiedName())) {
            throw new CantRunException(LocalizeValue.localizeTODO("Cannot test anonymous or local class \"" + data.getMainClassName() + '\"'));
        }
        calculateDependencies(null, classes, getSearchScope(), psiClass);
    }

    @Override
    public String getGeneratedName() {
        return JavaExecutionUtil.getPresentableClassName(myConfig.getOptions().getMainClassName());
    }

    @Override
    public String getActionName() {
        return JavaExecutionUtil.getShortClassName(myConfig.getOptions().getMainClassName());
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        TestNGConfigurationOptions data = myConfig.getOptions();
        SourceScope scope = myConfig.getTestSearchScope().getSourceScope(myConfig);
        if (scope == null) {
            throw new RuntimeConfigurationException(LocalizeValue.localizeTODO("Invalid scope specified"));
        }
        PsiManager manager = PsiManager.getInstance(myConfig.getProject());
        PsiClass psiClass = ClassUtil.findPsiClass(manager, data.getMainClassName(), null, true, scope.getGlobalSearchScope());
        if (psiClass == null) {
            throw new RuntimeConfigurationException(LocalizeValue.localizeTODO("Class '" + data.getMainClassName() + "' not found"));
        }
    }

    @Override
    public boolean isConfiguredByElement(PsiElement element) {
        element = PsiTreeUtil.getParentOfType(element, PsiModifierListOwner.class, false);
        return element instanceof PsiClass psiClass
            && Comparing.strEqual(myConfig.getOptions().getMainClassName(), JavaExecutionUtil.getRuntimeQualifiedName(psiClass));
    }
}
