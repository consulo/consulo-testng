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

import com.intellij.java.execution.JavaExecutionUtil;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.configuration.TestNGConfigurationEditor;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashSet;

/**
 * @author Hani Suleiman
 */
public class TestNGConfigurationModel {
    private static final Logger LOGGER = Logger.getInstance("TestNG Runner");

    private @Nullable TestNGConfigurationEditor<?> myEditor;
    private TestType myType;
    @SuppressWarnings("unchecked")
    private final ValueComponent<String>[] myTypeFields = new ValueComponent[6];
    private @Nullable ValueComponent<String> myPropertiesFileField;
    private @Nullable ValueComponent<String> myOutputDirectoryField;
    private final Project myProject;

    public TestNGConfigurationModel(Project project) {
        myType = TestType.CLASS;
        myProject = project;
    }

    public void setTypeField(TestType type, ValueComponent<String> field) {
        myTypeFields[type.getValue()] = field;
    }

    public void setPropertiesFileField(ValueComponent<String> field) {
        myPropertiesFileField = field;
    }

    public void setOutputDirectoryField(ValueComponent<String> field) {
        myOutputDirectoryField = field;
    }

    @RequiredUIAccess
    public void setType(TestType type) {
        myType = type;
        updateEditorType(type);
    }

    public TestType getType() {
        return myType;
    }

    @RequiredUIAccess
    private void updateEditorType(TestType type) {
        TestNGConfigurationEditor<?> editor = myEditor;
        if (editor != null) {
            editor.onTypeChanged(type);
        }
    }

    public void setListener(TestNGConfigurationEditor<?> editor) {
        myEditor = editor;
    }

    public Project getProject() {
        return myProject;
    }

    public void apply(TestNGConfiguration config) {
        boolean isGenerated = config.isGeneratedName();
        apply(config.getPersistantData());
        if (isGenerated && !JavaExecutionUtil.isNewName(config.getName())) {
            config.setGeneratedName();
        }
    }

    private void apply(TestData data) {
        data.TEST_OBJECT = myType.getType();
        if (TestType.GROUP == myType) {
            data.GROUP_NAME = getText(TestType.GROUP);
            data.PACKAGE_NAME = "";
            data.MAIN_CLASS_NAME = "";
            data.METHOD_NAME = "";
            data.SUITE_NAME = "";
        }
        else if (TestType.PACKAGE == myType) {
            data.PACKAGE_NAME = getText(TestType.PACKAGE);
            data.GROUP_NAME = "";
            data.MAIN_CLASS_NAME = "";
            data.METHOD_NAME = "";
            data.SUITE_NAME = "";
        }
        else if (TestType.METHOD == myType || TestType.CLASS == myType || TestType.SOURCE == myType) {
            String className = getText(TestType.CLASS);
            data.GROUP_NAME = "";
            data.SUITE_NAME = "";
            if (TestType.METHOD == myType || TestType.SOURCE == myType) {
                data.METHOD_NAME = getText(TestType.METHOD);
            }

            data.MAIN_CLASS_NAME = className;
            data.PACKAGE_NAME = StringUtil.getPackageName(className);
        }
        else if (TestType.SUITE == myType) {
            data.SUITE_NAME = getText(TestType.SUITE);
            data.PACKAGE_NAME = "";
            data.GROUP_NAME = "";
            data.MAIN_CLASS_NAME = "";
            data.METHOD_NAME = "";
        }
        else if (TestType.PATTERN == myType) {
            LinkedHashSet<String> set = new LinkedHashSet<>();
            String[] patterns = getText(TestType.PATTERN).split("\\|\\|");
            for (String pattern : patterns) {
                if (!pattern.isEmpty()) {
                    set.add(pattern);
                }
            }
            data.setPatterns(set);
        }

        data.PROPERTIES_FILE = getText(myPropertiesFileField);
        data.OUTPUT_DIRECTORY = getText(myOutputDirectoryField);
    }

    private String getText(TestType type) {
        return getText(myTypeFields[type.getValue()]);
    }

    private static String getText(@Nullable ValueComponent<String> field) {
        return field == null ? "" : StringUtil.notNullize(field.getValue());
    }

    @RequiredUIAccess
    public void reset(TestNGConfiguration config) {
        TestData data = config.getPersistantData();
        setType(data.TEST_OBJECT);
        setTypeValue(TestType.PACKAGE, data.getPackageName());
        setTypeValue(TestType.CLASS, data.getMainClassName());
        setTypeValue(TestType.METHOD, data.getMethodName());
        setTypeValue(TestType.GROUP, data.getGroupName());
        setTypeValue(TestType.SUITE, data.getSuiteName());
        setTypeValue(TestType.PATTERN, StringUtil.join(data.getPatterns(), "||"));

        setText(myPropertiesFileField, data.getPropertiesFile());
        setText(myOutputDirectoryField, data.getOutputDirectory());
    }

    @RequiredUIAccess
    private void setTypeValue(TestType type, String value) {
        setText(myTypeFields[type.getValue()], value);
    }

    @RequiredUIAccess
    private static void setText(@Nullable ValueComponent<String> field, String value) {
        if (field != null) {
            field.setValue(value);
        }
    }

    @RequiredUIAccess
    private void setType(String type) {
        try {
            setType(TestType.valueOf(type));
        }
        catch (IllegalArgumentException e) {
            LOGGER.debug("Invalid test type of " + type + " found.");
            setType(TestType.CLASS);
        }
    }
}
