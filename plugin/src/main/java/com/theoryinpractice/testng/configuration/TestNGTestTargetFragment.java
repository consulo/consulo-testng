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

import com.intellij.java.execution.JavaExecutionUtil;
import com.intellij.java.execution.impl.MethodListDlg;
import com.intellij.java.execution.impl.testDiscovery.TestDiscoveryExtension;
import com.intellij.java.language.impl.JavaFileType;
import com.intellij.java.language.impl.ui.JavaReferenceEditorUtil;
import com.intellij.java.language.psi.JavaCodeFragment;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiJavaCodeReferenceElement;
import com.intellij.java.language.psi.PsiMethod;
import com.theoryinpractice.testng.MessageInfoException;
import com.theoryinpractice.testng.configuration.browser.GroupBrowser;
import com.theoryinpractice.testng.configuration.browser.PackageBrowser;
import com.theoryinpractice.testng.configuration.browser.SuiteBrowser;
import com.theoryinpractice.testng.configuration.browser.TestClassBrowser;
import com.theoryinpractice.testng.model.TestType;
import com.theoryinpractice.testng.util.TestNGUtil;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.test.TestSearchScope;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.editor.ui.awt.TextFieldCompletionProvider;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.testng.localize.TestNGLocalize;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.HasSuffixComponent;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.RadioGroup;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.FormBuilder;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The test kind of a {@link TestNGConfiguration} with its target (package, class, method, group, suite or pattern) and the
 * search scope.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
class TestNGTestTargetFragment implements RunConfigurationFragment<TestNGConfiguration> {
    @Override
    public String getId() {
        return "testng.target";
    }

    @Override
    public LocalizeValue getName() {
        return TestNGLocalize.runConfigurationFragmentTestKind();
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.FIRST;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<TestNGConfiguration> createEditor(Project project, Disposable parent) {
        return new Editor(project, parent);
    }

    private static boolean isScopeShown(TestType type) {
        return type == TestType.PACKAGE || type == TestType.GROUP || type == TestType.SUITE || type == TestType.PATTERN;
    }

    private static TestType parseType(String type) {
        for (TestType testType : TestType.values()) {
            if (testType.getType().equals(type)) {
                return testType;
            }
        }
        return TestType.CLASS;
    }

    private record Row(Label label, Component field) {
        @RequiredUIAccess
        static Row add(FormBuilder builder, LocalizeValue text, Component field) {
            Label label = Label.create(text);
            builder.addLabeled(label, field);
            return new Row(label, field);
        }

        @RequiredUIAccess
        void setVisible(boolean visible) {
            label.setVisible(visible);
            field.setVisible(visible);
        }
    }

    private static final class Editor implements RunConfigurationFragmentEditor<TestNGConfiguration> {
        private final Project myProject;
        private final TestNGModuleHolder myModuleHolder;
        private final TestClassBrowser myTestClassBrowser;

        private final ComboBox<TestType> myTypeChooser;
        private final EditorBox myPackageField;
        private final EditorBox myClassField;
        private final EditorBox myMethodField;
        private final TextBox myGroupField;
        private final FileChooserTextBoxBuilder.Controller mySuiteField;
        private final TextBox myPatternField;
        private final RadioGroup<TestSearchScope> myScopeGroup;
        private final Component myForm;

        private final Row myPackageRow;
        private final Row myClassRow;
        private final Row myMethodRow;
        private final Row myGroupRow;
        private final Row mySuiteRow;
        private final Row myPatternRow;
        private final Row myScopeRow;

        @RequiredUIAccess
        private Editor(Project project, Disposable parent) {
            myProject = project;
            myModuleHolder = new TestNGModuleHolder(project);
            myTestClassBrowser = new TestClassBrowser(project, myModuleHolder);

            List<TestType> types = new ArrayList<>();
            for (TestType type : TestType.values()) {
                if (type != TestType.SOURCE || TestDiscoveryExtension.TESTDISCOVERY_ENABLED) {
                    types.add(type);
                }
            }
            myTypeChooser = ComboBox.create(types);
            myTypeChooser.setTextRenderer(type -> type == null ? LocalizeValue.empty() : type.getPresentableName());

            myPackageField = createReferenceField(false, JavaCodeFragment.VisibilityChecker.EVERYTHING_VISIBLE);
            myClassField = createReferenceField(true, this::isTestClassVisible);
            myMethodField = createMethodField();
            myGroupField = TextBox.create();

            FileChooserDescriptor suiteDescriptor = SuiteBrowser.createSuiteDescriptor();
            mySuiteField = FileChooserTextBoxBuilder.create(project)
                .uiDisposable(parent)
                .fileChooserDescriptor(suiteDescriptor)
                .dialogTitle(suiteDescriptor.getTitle())
                .dialogDescription(suiteDescriptor.getDescription())
                .build();

            myPatternField = TextBox.create();

            myScopeGroup = RadioGroup.create();
            VerticalLayout scopes = VerticalLayout.create(Space.NONE);
            scopes.add(myScopeGroup.newButton(ExecutionLocalize.junitConfigurationInWholeProjectRadio(), TestSearchScope.WHOLE_PROJECT));
            scopes.add(myScopeGroup.newButton(ExecutionLocalize.junitConfigurationInSingleModuleRadio(), TestSearchScope.SINGLE_MODULE));
            scopes.add(myScopeGroup.newButton(
                ExecutionLocalize.junitConfigurationAcrossModuleDependenciesRadio(),
                TestSearchScope.MODULE_WITH_DEPENDENCIES
            ));
            myScopeGroup.setValue(TestSearchScope.WHOLE_PROJECT, false);

            FormBuilder builder = FormBuilder.create();
            myClassRow = Row.add(builder, ExecutionLocalize.junitConfigurationClassLabel(), myClassField);
            myMethodRow = Row.add(builder, ExecutionLocalize.junitConfigurationMethodLabel(), myMethodField);
            mySuiteRow = Row.add(builder, TestNGLocalize.testngConfigurationSuiteLabel(), mySuiteField.getComponent());
            myGroupRow = Row.add(builder, TestNGLocalize.testngConfigurationGroupLabel(), myGroupField);
            myPatternRow = Row.add(builder, TestNGLocalize.testngConfigurationPatternLabel(), myPatternField);
            myPackageRow = Row.add(builder, ExecutionLocalize.junitConfigurationPackageLabel(), myPackageField);
            myScopeRow = Row.add(builder, ExecutionLocalize.junitConfigurationSearchForTestsLabel(), scopes);
            myForm = builder.build();

            if (!project.getApplication().isUnifiedApplication()) {
                installChooserActions();
            }

            myTypeChooser.addValueListener(event -> {
                TestType type = event.getValue();
                if (type != null) {
                    updateRows(type);
                }
            });
            updateRows(TestType.CLASS);
        }

        @RequiredUIAccess
        @Override
        public void addRows(RunConfigurationFragmentLayout layout) {
            layout.addLabeled(ExecutionLocalize.junitConfigurationConfigureJunitTestKindLabel(), myTypeChooser);
            layout.addUnlabeled(myForm);
        }

        @RequiredUIAccess
        @Override
        public void reset(TestNGConfiguration target) {
            myModuleHolder.setModule(target.getConfigurationModule().getModule());

            TestNGConfigurationOptions options = target.getOptions();
            TestType type = parseType(options.getTestObject());
            myTypeChooser.setValue(type, false);
            myPackageField.setValue(options.getPackageName());
            myClassField.setValue(options.getMainClassName());
            myMethodField.setValue(options.getMethodName());
            myGroupField.setValue(options.getGroupName(), false);
            mySuiteField.setValue(options.getSuiteName(), false);
            myPatternField.setValue(StringUtil.join(options.getPatterns(), "||"), false);
            TestSearchScope scope = target.getTestSearchScope();
            myScopeGroup.setValue(
                scope == TestSearchScope.SINGLE_MODULE || scope == TestSearchScope.MODULE_WITH_DEPENDENCIES ? scope : TestSearchScope.WHOLE_PROJECT,
                false
            );
            updateRows(type);
        }

        @RequiredUIAccess
        @Override
        public void apply(TestNGConfiguration target) {
            myModuleHolder.setModule(target.getConfigurationModule().getModule());

            boolean generatedName = target.isGeneratedName();
            TestNGConfigurationOptions options = target.getOptions();
            TestType type = getType();
            options.setTestObject(type.getType());
            if (type == TestType.GROUP) {
                options.setGroupName(getText(myGroupField.getValue()));
                options.setPackageName("");
                options.setMainClassName("");
                options.setMethodName("");
                options.setSuiteName("");
            }
            else if (type == TestType.PACKAGE) {
                options.setPackageName(getText(myPackageField.getValue()));
                options.setGroupName("");
                options.setMainClassName("");
                options.setMethodName("");
                options.setSuiteName("");
            }
            else if (type == TestType.METHOD || type == TestType.CLASS || type == TestType.SOURCE) {
                String className = getText(myClassField.getValue());
                options.setGroupName("");
                options.setSuiteName("");
                if (type == TestType.METHOD || type == TestType.SOURCE) {
                    options.setMethodName(getText(myMethodField.getValue()));
                }
                options.setMainClassName(className);
                options.setPackageName(StringUtil.getPackageName(className));
            }
            else if (type == TestType.SUITE) {
                options.setSuiteName(getText(mySuiteField.getValue()));
                options.setPackageName("");
                options.setGroupName("");
                options.setMainClassName("");
                options.setMethodName("");
            }
            else if (type == TestType.PATTERN) {
                Set<String> patterns = new LinkedHashSet<>();
                for (String pattern : getText(myPatternField.getValue()).split("\\|\\|")) {
                    if (!pattern.isEmpty()) {
                        patterns.add(pattern);
                    }
                }
                target.setPatterns(patterns);
            }

            if (isScopeShown(type)) {
                TestSearchScope scope = myScopeGroup.getValue();
                target.setTestSearchScope(scope == null ? TestSearchScope.WHOLE_PROJECT : scope);
            }
            else {
                target.setTestSearchScope(TestSearchScope.MODULE_WITH_DEPENDENCIES);
            }

            if (generatedName && !JavaExecutionUtil.isNewName(target.getName())) {
                target.setGeneratedName();
            }
        }

        private TestType getType() {
            TestType type = myTypeChooser.getValue();
            return type == null ? TestType.CLASS : type;
        }

        private static String getText(@Nullable String value) {
            return StringUtil.notNullize(value).trim();
        }

        @RequiredUIAccess
        private void updateRows(TestType type) {
            boolean method = type == TestType.METHOD || type == TestType.SOURCE;
            myPackageRow.setVisible(type == TestType.PACKAGE);
            myClassRow.setVisible(type == TestType.CLASS || method);
            myMethodRow.setVisible(method);
            myGroupRow.setVisible(type == TestType.GROUP);
            mySuiteRow.setVisible(type == TestType.SUITE);
            myPatternRow.setVisible(type == TestType.PATTERN);
            myScopeRow.setVisible(isScopeShown(type));
        }

        private JavaCodeFragment.VisibilityChecker.Visibility isTestClassVisible(PsiElement declaration, PsiElement place) {
            if (declaration instanceof PsiClass && place.getParent() instanceof PsiJavaCodeReferenceElement) {
                return JavaCodeFragment.VisibilityChecker.Visibility.VISIBLE;
            }
            try {
                if (declaration instanceof PsiClass psiClass && myTestClassBrowser.getFilter().isAccepted(psiClass)) {
                    return JavaCodeFragment.VisibilityChecker.Visibility.VISIBLE;
                }
            }
            catch (MessageInfoException e) {
                return JavaCodeFragment.VisibilityChecker.Visibility.NOT_VISIBLE;
            }
            return JavaCodeFragment.VisibilityChecker.Visibility.NOT_VISIBLE;
        }

        @RequiredUIAccess
        private EditorBox createReferenceField(boolean classesAccepted, JavaCodeFragment.VisibilityChecker visibilityChecker) {
            EditorBox field = myProject.getApplication().getInstance(EditorBoxBuilderFactory.class).create(myProject).build();
            if (myProject.isDefault()) {
                return field;
            }

            CoroutineScope.launchAsync(
                myProject.coroutineContext(),
                () -> Coroutine
                    .first(ReadLock.<Void, @Nullable Document>apply(
                        ignored -> JavaReferenceEditorUtil.createDocument("", myProject, classesAccepted, visibilityChecker)
                    ))
                    .then(UIAction.<@Nullable Document, Void>apply(document -> {
                        if (document != null) {
                            String text = StringUtil.notNullize(field.getValue());
                            field.setDocument(document, JavaFileType.INSTANCE);
                            field.setValue(text);
                        }
                        return null;
                    }))
            );
            return field;
        }

        @RequiredUIAccess
        private EditorBox createMethodField() {
            TextFieldCompletionProvider completionProvider = new TextFieldCompletionProvider() {
                @Override
                public void addCompletionVariants(String text, int offset, String prefix, CompletionResultSet result) {
                    PsiClass testClass = findTestClass();
                    if (testClass == null) {
                        return;
                    }

                    for (PsiMethod psiMethod : testClass.getAllMethods()) {
                        if (TestNGUtil.hasTest(psiMethod)) {
                            result.addElement(LookupElementBuilder.create(psiMethod.getName()));
                        }
                    }
                }
            };

            return myProject.getApplication()
                .getInstance(EditorBoxBuilderFactory.class)
                .create(myProject)
                .completion(completionProvider)
                .build();
        }

        private @Nullable PsiClass findTestClass() {
            String className = StringUtil.notNullize(myClassField.getValue());
            if (StringUtil.isEmptyOrSpaces(className)) {
                return null;
            }
            return myModuleHolder.findClass(className);
        }

        @RequiredUIAccess
        private void installChooserActions() {
            installAction(myPackageField, "TestNGConfigurationEditorPackage", new ChooserAction(
                myProject,
                TestNGLocalize.testngConfigurationChoosePackageAction(),
                PlatformIconGroup.nodesPackage(),
                () -> {
                    String packageName = new PackageBrowser(myProject).showDialog();
                    if (packageName != null) {
                        myPackageField.setValue(packageName);
                    }
                }
            ));

            installAction(myClassField, "TestNGConfigurationEditorClass", new ChooserAction(
                myProject,
                ExecutionLocalize.chooseTestClassDialogTitle(),
                PlatformIconGroup.nodesClass(),
                () -> {
                    String className = myTestClassBrowser.chooseClass(myClassField.getValue());
                    if (className != null) {
                        myClassField.setValue(className);
                    }
                }
            ));

            installAction(myMethodField, "TestNGConfigurationEditorMethod", new ChooserAction(
                myProject,
                TestNGLocalize.testngConfigurationChooseMethodAction(),
                PlatformIconGroup.nodesMethod(),
                this::chooseMethod
            ));

            installAction(myGroupField, "TestNGConfigurationEditorGroup", new ChooserAction(
                myProject,
                TestNGLocalize.testngChooseTestGroup(),
                PlatformIconGroup.nodesTag(),
                () -> {
                    GroupBrowser groupBrowser = new GroupBrowser(myProject, myModuleHolder);
                    String groupName = groupBrowser.chooseGroup((JComponent) TargetAWT.to(myGroupField));
                    if (groupName != null) {
                        myGroupField.setValue(groupName);
                    }
                }
            ));

            installAction(myPatternField, "TestNGConfigurationEditorPattern", new ChooserAction(
                myProject,
                TestNGLocalize.testngConfigurationAddTestClassAction(),
                PlatformIconGroup.generalAdd(),
                () -> {
                    String className = myTestClassBrowser.chooseClass(null);
                    if (className != null) {
                        String text = StringUtil.notNullize(myPatternField.getValue());
                        myPatternField.setValue(text + (text.isEmpty() ? "" : "||") + className);
                    }
                }
            ));
        }

        @RequiredUIAccess
        private void chooseMethod() {
            String className = StringUtil.notNullize(myClassField.getValue());
            if (StringUtil.isEmptyOrSpaces(className)) {
                MessageBoxes.okInfo(ExecutionLocalize.setClassNameMessage())
                    .title(ExecutionLocalize.cannotBrowseMethodDialogTitle())
                    .showAsync(myMethodField);
                return;
            }

            PsiClass testClass = myModuleHolder.findClass(className);
            if (testClass == null) {
                MessageBoxes.okInfo(ExecutionLocalize.classDoesNotExistsErrorMessage(className))
                    .title(ExecutionLocalize.cannotBrowseMethodDialogTitle())
                    .showAsync(myMethodField);
                return;
            }

            MethodListDlg dialog = new MethodListDlg(testClass, TestNGUtil::hasTest, (JComponent) TargetAWT.to(myMethodField));
            if (dialog.showAndGet()) {
                PsiMethod method = dialog.getSelected();
                if (method != null) {
                    myMethodField.setValue(method.getName());
                }
            }
        }

        @RequiredUIAccess
        private <C extends Component & HasSuffixComponent> void installAction(C field, String place, AnAction action) {
            ActionToolbar toolbar = myProject.getApplication()
                .getInstance(ActionToolbarFactory.class)
                .createActionToolbar(place, ActionGroup.newImmutableBuilder().add(action).build(), ActionToolbar.Style.INPLACE);
            toolbar.setTargetUIComponent(field);
            toolbar.updateActionsAsync();
            field.setSuffixComponent(toolbar.getUIComponent());
        }
    }

    private static final class ChooserAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private final Project myProject;
        private final Runnable myChooser;

        private ChooserAction(Project project, LocalizeValue text, Image icon, @RequiredUIAccess Runnable chooser) {
            super(text, LocalizeValue.empty(), icon);
            myProject = project;
            myChooser = chooser;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(!myProject.isDefault());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            myChooser.run();
        }
    }
}
