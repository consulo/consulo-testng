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

import com.dslplatform.json.CompiledJson;
import com.theoryinpractice.testng.model.TestType;
import consulo.execution.configuration.RunConfigurationOptions;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.MapAnnotation;
import consulo.util.xml.serializer.annotation.Property;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a {@link TestNGConfiguration} runs: the test kind with its target, the search scope and the TestNG options.
 * String getters return an empty string for an unset value.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@CompiledJson
public class TestNGConfigurationOptions extends RunConfigurationOptions {
    private String myTestObject = TestType.CLASS.getType();
    private @Nullable String mySuiteName;
    private @Nullable String myPackageName;
    private @Nullable String myMainClassName;
    private @Nullable String myMethodName;
    private @Nullable String myGroupName;
    private @Nullable String myOutputDirectory;
    private @Nullable String myPropertiesFile;
    private String mySearchScope = TestNGConfiguration.MODULE_WITH_DEPENDENCIES_SCOPE;
    private boolean myUseDefaultReporters;
    private List<String> myPatterns = new ArrayList<>();
    private List<String> myListeners = new ArrayList<>();
    private Map<String, String> myProperties = new LinkedHashMap<>();

    /**
     * One of {@link TestType#getType()}.
     */
    @Attribute("testKind")
    public String getTestObject() {
        return myTestObject;
    }

    public void setTestObject(@Nullable String testObject) {
        myTestObject = StringUtil.isEmptyOrSpaces(testObject) ? TestType.CLASS.getType() : testObject;
    }

    @Attribute("suite")
    public String getSuiteName() {
        return StringUtil.notNullize(mySuiteName);
    }

    public void setSuiteName(@Nullable String suiteName) {
        mySuiteName = StringUtil.nullize(suiteName, true);
    }

    @Attribute("package")
    public String getPackageName() {
        return StringUtil.notNullize(myPackageName);
    }

    public void setPackageName(@Nullable String packageName) {
        myPackageName = StringUtil.nullize(packageName, true);
    }

    /**
     * The runtime qualified name of the test class.
     */
    @Attribute("class")
    public String getMainClassName() {
        return StringUtil.notNullize(myMainClassName);
    }

    public void setMainClassName(@Nullable String mainClassName) {
        myMainClassName = StringUtil.nullize(mainClassName, true);
    }

    @Attribute("method")
    public String getMethodName() {
        return StringUtil.notNullize(myMethodName);
    }

    public void setMethodName(@Nullable String methodName) {
        myMethodName = StringUtil.nullize(methodName, true);
    }

    /**
     * Comma separated group names.
     */
    @Attribute("group")
    public String getGroupName() {
        return StringUtil.notNullize(myGroupName);
    }

    public void setGroupName(@Nullable String groupName) {
        myGroupName = StringUtil.nullize(groupName, true);
    }

    @Attribute("outputDirectory")
    public String getOutputDirectory() {
        return StringUtil.notNullize(myOutputDirectory);
    }

    public void setOutputDirectory(@Nullable String outputDirectory) {
        myOutputDirectory = StringUtil.nullize(outputDirectory, true);
    }

    @Attribute("propertiesFile")
    public String getPropertiesFile() {
        return StringUtil.notNullize(myPropertiesFile);
    }

    public void setPropertiesFile(@Nullable String propertiesFile) {
        myPropertiesFile = StringUtil.nullize(propertiesFile, true);
    }

    /**
     * The name of the {@link consulo.execution.test.TestSearchScope} for package, group, suite and pattern tests.
     */
    @Attribute("searchScope")
    public String getSearchScope() {
        return mySearchScope;
    }

    public void setSearchScope(@Nullable String searchScope) {
        mySearchScope = StringUtil.isEmptyOrSpaces(searchScope) ? TestNGConfiguration.MODULE_WITH_DEPENDENCIES_SCOPE : searchScope;
    }

    @Attribute("useDefaultReporters")
    public boolean isUseDefaultReporters() {
        return myUseDefaultReporters;
    }

    public void setUseDefaultReporters(boolean useDefaultReporters) {
        myUseDefaultReporters = useDefaultReporters;
    }

    /**
     * Test classes, optionally followed by {@code ,method}, of a pattern test.
     */
    @Property(surroundWithTag = false)
    @AbstractCollection(surroundWithTag = false, elementTag = "pattern", elementValueAttribute = "testClass")
    public List<String> getPatterns() {
        return myPatterns;
    }

    public void setPatterns(List<String> patterns) {
        myPatterns = new ArrayList<>(patterns);
    }

    /**
     * Qualified names of the TestNG listener classes.
     */
    @Property(surroundWithTag = false)
    @AbstractCollection(surroundWithTag = false, elementTag = "listener", elementValueAttribute = "class")
    public List<String> getListeners() {
        return myListeners;
    }

    public void setListeners(List<String> listeners) {
        myListeners = new ArrayList<>(listeners);
    }

    /**
     * Suite parameters passed to TestNG.
     */
    @Property(surroundWithTag = false)
    @MapAnnotation(
        surroundWithTag = false,
        surroundKeyWithTag = false,
        surroundValueWithTag = false,
        keyAttributeName = "name",
        valueAttributeName = "value",
        entryTagName = "property"
    )
    public Map<String, String> getProperties() {
        return myProperties;
    }

    public void setProperties(Map<String, String> properties) {
        myProperties = new LinkedHashMap<>(properties);
    }
}
