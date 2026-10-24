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

import consulo.execution.configuration.fragment.TagRunConfigurationFragment;
import consulo.localize.LocalizeValue;
import consulo.testng.localize.TestNGLocalize;

/**
 * Keeps the default TestNG reporters next to the listeners of the configuration.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
class TestNGUseDefaultReportersFragment implements TagRunConfigurationFragment<TestNGConfiguration> {
    @Override
    public String getId() {
        return "testng.use.default.reporters";
    }

    @Override
    public LocalizeValue getName() {
        return TestNGLocalize.runConfigurationFragmentUseDefaultReporters();
    }

    @Override
    public LocalizeValue getGroup() {
        return TestNGLocalize.runConfigurationFragmentGroupTestng();
    }

    @Override
    public boolean isSet(TestNGConfiguration target) {
        return target.getOptions().isUseDefaultReporters();
    }

    @Override
    public void set(TestNGConfiguration target, boolean value) {
        target.getOptions().setUseDefaultReporters(value);
    }
}
