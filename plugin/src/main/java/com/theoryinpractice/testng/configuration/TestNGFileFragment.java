/*
 * Copyright 2013-2026 consulo.io
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

import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.testng.localize.TestNGLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;

/**
 * A path option of a {@link TestNGConfiguration}, shown while it is set.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
abstract class TestNGFileFragment implements RunConfigurationFragment<TestNGConfiguration> {
    protected abstract LocalizeValue getLabel();

    protected abstract String getValue(TestNGConfigurationOptions options);

    protected abstract void setValue(TestNGConfigurationOptions options, String value);

    protected abstract FileChooserTextBoxBuilder configure(FileChooserTextBoxBuilder builder);

    @Override
    public LocalizeValue getGroup() {
        return TestNGLocalize.runConfigurationFragmentGroupTestng();
    }

    @Override
    public boolean isVisibleByDefault(TestNGConfiguration target) {
        return !getValue(target.getOptions()).isEmpty();
    }

    @Override
    public void applyHidden(TestNGConfiguration target) {
        setValue(target.getOptions(), "");
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<TestNGConfiguration> createEditor(Project project, Disposable parent) {
        FileChooserTextBoxBuilder.Controller field = configure(FileChooserTextBoxBuilder.create(project).uiDisposable(parent)).build();

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(getLabel(), field.getComponent());
            }

            @RequiredUIAccess
            @Override
            public void reset(TestNGConfiguration target) {
                field.setValue(getValue(target.getOptions()), false);
            }

            @RequiredUIAccess
            @Override
            public void apply(TestNGConfiguration target) {
                setValue(target.getOptions(), StringUtil.notNullize(field.getValue()).trim());
            }
        };
    }
}
