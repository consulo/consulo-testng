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

import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.testng.localize.TestNGLocalize;

/**
 * A properties file with suite parameters; the parameters of the configuration override it.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
class TestNGPropertiesFileFragment extends TestNGFileFragment {
    @Override
    public String getId() {
        return "testng.properties.file";
    }

    @Override
    public LocalizeValue getName() {
        return TestNGLocalize.runConfigurationFragmentPropertiesFile();
    }

    @Override
    protected LocalizeValue getLabel() {
        return TestNGLocalize.testngConfigurationPropertiesFile();
    }

    @Override
    protected String getValue(TestNGConfigurationOptions options) {
        return options.getPropertiesFile();
    }

    @Override
    protected void setValue(TestNGConfigurationOptions options, String value) {
        options.setPropertiesFile(value);
    }

    @Override
    protected FileChooserTextBoxBuilder configure(FileChooserTextBoxBuilder builder) {
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
            .withFileFilter(file -> "properties".equals(file.getExtension()))
            .withExtensionFilter("properties");
        return builder
            .fileChooserDescriptor(descriptor)
            .dialogTitle(TestNGLocalize.testngBrowseButtonTitle())
            .dialogDescription(TestNGLocalize.testngSelectPropertiesFile());
    }
}
