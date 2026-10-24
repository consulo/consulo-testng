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

import com.intellij.java.execution.impl.junit.JavaRuntimeConfigurationProducerBase;
import com.intellij.java.execution.impl.junit2.info.LocationUtil;
import com.intellij.java.language.psi.PsiJavaPackage;
import com.theoryinpractice.testng.model.TestType;
import com.theoryinpractice.testng.util.TestNGUtil;
import consulo.execution.action.ConfigurationContext;
import consulo.execution.action.Location;
import consulo.execution.configuration.ConfigurationType;
import consulo.language.psi.PsiElement;
import consulo.util.lang.ref.SimpleReference;

public abstract class AbstractTestNGPackageConfigurationProducer extends TestNGConfigurationProducer {

    protected AbstractTestNGPackageConfigurationProducer(ConfigurationType configurationType) {
        super(configurationType);
    }

    @Override
    protected boolean setupConfigurationFromContext(TestNGConfiguration configuration, ConfigurationContext context, SimpleReference<PsiElement> sourceElement) {
        final PsiElement element = context.getPsiLocation();
        PsiJavaPackage aPackage = JavaRuntimeConfigurationProducerBase.checkPackage(element);
        if (aPackage == null) {
            return false;
        }
        final Location location = context.getLocation();
        if (location == null) {
            return false;
        }
        if (!LocationUtil.isJarAttached(location, aPackage, TestNGUtil.TEST_ANNOTATION_FQN)) {
            return false;
        }
        final TestNGConfigurationOptions data = configuration.getOptions();
        data.setPackageName(aPackage.getQualifiedName());
        data.setTestObject(TestType.PACKAGE.getType());
        configuration.setTestSearchScope(setupPackageConfiguration(context, configuration, configuration.getTestSearchScope()));
        configuration.setGeneratedName();
        sourceElement.set(aPackage);
        return true;
    }

}