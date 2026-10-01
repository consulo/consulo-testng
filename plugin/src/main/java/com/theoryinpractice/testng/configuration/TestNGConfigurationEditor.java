/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import com.intellij.java.execution.impl.ui.CommonJavaParametersLayout;
import com.intellij.java.execution.impl.ui.UnifiedConfigurationModuleSelector;
import com.intellij.java.execution.impl.ui.UnifiedJrePathEditor;
import com.intellij.java.execution.impl.ui.UnifiedShortenCommandLineModeCombo;
import com.intellij.java.language.impl.JavaFileType;
import com.intellij.java.language.impl.ui.JavaReferenceEditorUtil;
import com.intellij.java.language.psi.JavaCodeFragment;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiJavaCodeReferenceElement;
import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.util.TreeClassChooser;
import com.intellij.java.language.util.TreeClassChooserFactory;
import com.theoryinpractice.testng.MessageInfoException;
import com.theoryinpractice.testng.configuration.browser.GroupBrowser;
import com.theoryinpractice.testng.configuration.browser.PackageBrowser;
import com.theoryinpractice.testng.configuration.browser.SuiteBrowser;
import com.theoryinpractice.testng.configuration.browser.TestClassBrowser;
import com.theoryinpractice.testng.model.TestData;
import com.theoryinpractice.testng.model.TestListenerFilter;
import com.theoryinpractice.testng.model.TestNGConfigurationModel;
import com.theoryinpractice.testng.model.TestType;
import com.theoryinpractice.testng.util.TestNGUtil;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.document.Document;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.test.TestSearchScope;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.java.execution.localize.JavaExecutionLocalize;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.editor.ui.awt.TextFieldCompletionProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.testng.localize.TestNGLocalize;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.HasSuffixComponent;
import consulo.ui.InputBoxBuilder;
import consulo.ui.Label;
import consulo.ui.ListBox;
import consulo.ui.MessageBoxes;
import consulo.ui.RadioGroup;
import consulo.ui.Space;
import consulo.ui.Tab;
import consulo.ui.Table;
import consulo.ui.TableItemEditor;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.UIAction;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.ui.util.FormBuilder;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class TestNGConfigurationEditor<T extends TestNGConfiguration> extends SettingsEditor<T> {
    private final Project myProject;

    private @Nullable TestNGParametersLayout myLayout;

    public TestNGConfigurationEditor(Project project) {
        myProject = project;
    }

    @Override
    @RequiredUIAccess
    protected Component createUIComponent() {
        TestNGParametersLayout layout = new TestNGParametersLayout();
        myLayout = layout;
        layout.build();
        layout.initialize();
        return layout.getComponent();
    }

    @Override
    @RequiredUIAccess
    protected void resetEditorFrom(T configuration) {
        TestNGParametersLayout layout = myLayout;
        if (layout != null) {
            layout.reset(configuration);
        }
    }

    @Override
    @RequiredUIAccess
    protected void applyEditorTo(T configuration) {
        TestNGParametersLayout layout = myLayout;
        if (layout != null) {
            layout.apply(configuration);
        }
    }

    @RequiredUIAccess
    public void onTypeChanged(TestType type) {
        TestNGParametersLayout layout = myLayout;
        if (layout != null) {
            layout.onTypeChanged(type);
        }
    }

    private static boolean isScopeShown(TestType type) {
        return type == TestType.PACKAGE || type == TestType.GROUP || type == TestType.SUITE || type == TestType.PATTERN;
    }

    @RequiredUIAccess
    private static void addTab(TabbedLayout tabs, LocalizeValue title, Component component) {
        Tab tab = tabs.createTab();
        tab.setRenderer((it, presentation) -> presentation.append(title));
        tabs.addTab(tab, component);
    }

    private record Row(Label label, Component field) {
        @RequiredUIAccess
        void setVisible(boolean visible) {
            label.setVisible(visible);
            field.setVisible(visible);
        }

        @RequiredUIAccess
        void setEnabled(boolean enabled) {
            label.setEnabled(enabled);
            field.setEnabled(enabled);
        }
    }

    private static final class PropertyRow {
        private String myName;
        private String myValue;

        private PropertyRow(String name, String value) {
            myName = name;
            myValue = value;
        }

        private String getName() {
            return myName;
        }

        private void setName(String name) {
            myName = name;
        }

        private String getValue() {
            return myValue;
        }

        private void setValue(String value) {
            myValue = value;
        }
    }

    private final class ChooserAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        private final Runnable myChooser;

        private ChooserAction(LocalizeValue text, Image icon, @RequiredUIAccess Runnable chooser) {
            super(text, LocalizeValue.empty(), icon);
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

    private class TestNGParametersLayout extends CommonJavaParametersLayout<T> {
        private final TestNGConfigurationModel myModel = new TestNGConfigurationModel(myProject);
        private final UnifiedConfigurationModuleSelector myModuleSelector;
        private final UnifiedJrePathEditor myJrePathEditor;
        private final UnifiedShortenCommandLineModeCombo myShortenCommandLineModeCombo;
        private final TestClassBrowser myTestClassBrowser;

        private final ComboBox<TestType> myTypeChooser;
        private final EditorBox myPackageField;
        private final EditorBox myClassField;
        private final EditorBox myMethodField;
        private final TextBox myGroupField;
        private final FileChooserTextBoxBuilder.Controller mySuiteField;
        private final TextBox myPatternField;
        private final RadioGroup<TestSearchScope> myScopeGroup;
        private final VerticalLayout myScopesLayout;
        private final FileChooserTextBoxBuilder.Controller myOutputDirectoryField;
        private final FileChooserTextBoxBuilder.Controller myPropertiesFileField;

        private final MutableFlatDataModel<PropertyRow> myPropertiesModel = FlatDataModel.of(new ArrayList<>());
        private final Table<PropertyRow> myPropertiesTable;
        private final MutableFlatDataModel<String> myListenersModel = FlatDataModel.of(new ArrayList<>());
        private final ListBox<String> myListenersList;
        private final CheckBox myUseDefaultReportersBox;

        private final Map<TestType, Row> myTestLocations = new EnumMap<>(TestType.class);
        private @Nullable Row myScopesRow;
        private @Nullable Row myModuleRow;

        @RequiredUIAccess
        private TestNGParametersLayout() {
            super(myProject.getApplication().getInstance(DialogService.class));
            setHasModuleMacro();

            myModuleSelector = new UnifiedConfigurationModuleSelector(myProject, JavaExecutionLocalize.runConfigurationModuleNone());
            myModuleSelector.addValueListener(this::setModuleContext);
            myJrePathEditor = new UnifiedJrePathEditor(TestNGConfigurationEditor.this);
            myShortenCommandLineModeCombo = new UnifiedShortenCommandLineModeCombo(myProject, myJrePathEditor, myModuleSelector);
            myTestClassBrowser = new TestClassBrowser(myProject, myModuleSelector);

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
            mySuiteField = FileChooserTextBoxBuilder.create(myProject)
                .fileChooserDescriptor(suiteDescriptor)
                .dialogTitle(suiteDescriptor.getTitle())
                .dialogDescription(suiteDescriptor.getDescription())
                .build();

            myPatternField = TextBox.create();

            myScopeGroup = RadioGroup.create();
            myScopesLayout = VerticalLayout.create(Space.NONE);
            myScopesLayout.add(myScopeGroup.newButton(
                ExecutionLocalize.junitConfigurationInWholeProjectRadio(),
                TestSearchScope.WHOLE_PROJECT
            ));
            myScopesLayout.add(myScopeGroup.newButton(
                ExecutionLocalize.junitConfigurationInSingleModuleRadio(),
                TestSearchScope.SINGLE_MODULE
            ));
            myScopesLayout.add(myScopeGroup.newButton(
                ExecutionLocalize.junitConfigurationAcrossModuleDependenciesRadio(),
                TestSearchScope.MODULE_WITH_DEPENDENCIES
            ));
            myScopeGroup.setValue(TestSearchScope.WHOLE_PROJECT, false);

            myOutputDirectoryField = FileChooserTextBoxBuilder.create(myProject)
                .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor())
                .dialogTitle(TestNGLocalize.testngOutputDirectoryButtonTitle())
                .dialogDescription(TestNGLocalize.testngSelectOutputDirectory())
                .build();

            FileChooserDescriptor propertiesDescriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withFileFilter(file -> "properties".equals(file.getExtension()))
                .withExtensionFilter("properties");
            myPropertiesFileField = FileChooserTextBoxBuilder.create(myProject)
                .fileChooserDescriptor(propertiesDescriptor)
                .dialogTitle(TestNGLocalize.testngBrowseButtonTitle())
                .dialogDescription(TestNGLocalize.testngSelectPropertiesFile())
                .build();

            myPropertiesTable = createPropertiesTable();
            myListenersList = ListBox.create(myListenersModel);
            myUseDefaultReportersBox = CheckBox.create(TestNGLocalize.testngConfigurationUseDefaultReportersOption());

            if (!myProject.getApplication().isUnifiedApplication()) {
                installChooserActions();
            }
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
            return myModuleSelector.findClass(className);
        }

        @RequiredUIAccess
        private Table<PropertyRow> createPropertiesTable() {
            Table<PropertyRow> table = Table.create(myPropertiesModel);
            table.addColumn(TestNGLocalize.testngParametersTableModelName(), PropertyRow::getName)
                .setEditor(new TableItemEditor<>() {
                    @Override
                    @RequiredUIAccess
                    public ValueComponent<String> createComponent(PropertyRow row) {
                        return TextBox.create(row.getName());
                    }

                    @Override
                    @RequiredUIAccess
                    public void commit(PropertyRow row, @Nullable String value) {
                        row.setName(StringUtil.notNullize(value));
                    }
                });
            table.addColumn(TestNGLocalize.testngParametersTableModelValue(), PropertyRow::getValue)
                .setEditor(new TableItemEditor<>() {
                    @Override
                    @RequiredUIAccess
                    public ValueComponent<String> createComponent(PropertyRow row) {
                        return TextBox.create(row.getValue());
                    }

                    @Override
                    @RequiredUIAccess
                    public void commit(PropertyRow row, @Nullable String value) {
                        row.setValue(StringUtil.notNullize(value));
                    }
                });
            return table;
        }

        @RequiredUIAccess
        private void installChooserActions() {
            installAction(myPackageField, "TestNGConfigurationEditorPackage", new ChooserAction(
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
                TestNGLocalize.testngConfigurationChooseMethodAction(),
                PlatformIconGroup.nodesMethod(),
                this::chooseMethod
            ));

            installAction(myGroupField, "TestNGConfigurationEditorGroup", new ChooserAction(
                TestNGLocalize.testngChooseTestGroup(),
                PlatformIconGroup.nodesTag(),
                () -> {
                    GroupBrowser groupBrowser = new GroupBrowser(myProject, myModuleSelector);
                    String groupName = groupBrowser.chooseGroup((JComponent) TargetAWT.to(myGroupField));
                    if (groupName != null) {
                        myGroupField.setValue(groupName);
                    }
                }
            ));

            installAction(myPatternField, "TestNGConfigurationEditorPattern", new ChooserAction(
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

            PsiClass testClass = myModuleSelector.findClass(className);
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
        private void addListener() {
            if (!myProject.getApplication().isUnifiedApplication()) {
                String className = selectListenerClass();
                if (className != null) {
                    appendListener(className);
                }
                return;
            }

            UIAccess uiAccess = UIAccess.current();
            InputBoxBuilder.text()
                .title(TestNGLocalize.testngConfigurationAddListenerDialogTitle())
                .text(TestNGLocalize.testngConfigurationListenerClassLabel())
                .showAsync(myListenersList)
                .whenComplete((value, error) -> {
                    if (error == null && !StringUtil.isEmptyOrSpaces(value)) {
                        uiAccess.give(() -> appendListener(value.trim()));
                    }
                });
        }

        @RequiredUIAccess
        private @Nullable String selectListenerClass() {
            Module module = myModuleSelector.getModule();
            GlobalSearchScope searchScope = module == null
                ? GlobalSearchScope.allScope(myProject)
                : GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
            TestListenerFilter filter = new TestListenerFilter(searchScope, myProject);

            TreeClassChooser chooser = myProject.getInstance(TreeClassChooserFactory.class).createWithInnerClassesScopeChooser(
                TestNGLocalize.testngConfigEditorDialogTitleChooseListenerClass().get(),
                filter.getScope(),
                filter,
                null
            );
            chooser.showDialog();
            PsiClass psiClass = chooser.getSelected();
            return psiClass == null ? null : JavaExecutionUtil.getRuntimeQualifiedName(psiClass);
        }

        @RequiredUIAccess
        private void appendListener(String className) {
            if (myListenersModel.indexOf(className) < 0) {
                myListenersModel.add(className);
            }
            myListenersList.setValue(className);
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

        @RequiredUIAccess
        private Row addRow(FormBuilder builder, LocalizeValue text, Component field) {
            Label label = Label.create(text);
            builder.addLabeled(label, field);
            return new Row(label, field);
        }

        @Override
        @RequiredUIAccess
        protected void addBefore(FormBuilder builder) {
            builder.addLabeled(ExecutionLocalize.junitConfigurationConfigureJunitTestKindLabel(), myTypeChooser);

            myTestLocations.put(TestType.CLASS, addRow(builder, ExecutionLocalize.junitConfigurationClassLabel(), myClassField));
            myTestLocations.put(TestType.METHOD, addRow(builder, ExecutionLocalize.junitConfigurationMethodLabel(), myMethodField));
            myTestLocations.put(
                TestType.SUITE,
                addRow(builder, TestNGLocalize.testngConfigurationSuiteLabel(), mySuiteField.getComponent())
            );
            myTestLocations.put(TestType.GROUP, addRow(builder, TestNGLocalize.testngConfigurationGroupLabel(), myGroupField));
            myTestLocations.put(TestType.PATTERN, addRow(builder, TestNGLocalize.testngConfigurationPatternLabel(), myPatternField));
            myTestLocations.put(TestType.PACKAGE, addRow(builder, ExecutionLocalize.junitConfigurationPackageLabel(), myPackageField));
            myScopesRow = addRow(builder, ExecutionLocalize.junitConfigurationSearchForTestsLabel(), myScopesLayout);
            builder.addLabeled(TestNGLocalize.testngConfigurationOutputDirectory(), myOutputDirectoryField.getComponent());

            super.addBefore(builder);
        }

        @Override
        @RequiredUIAccess
        protected void addAfter(FormBuilder builder) {
            myModuleRow = addRow(
                builder,
                JavaExecutionLocalize.applicationConfigurationUseClasspathAndJdkOfModuleLabel(),
                myModuleSelector.getComponent()
            );
            builder.addLabeled(JavaExecutionLocalize.runConfigurationJreLabel(), myJrePathEditor.getComponent());
            builder.addLabeled(
                JavaExecutionLocalize.applicationConfigurationShortenCommandLineLabel(),
                myShortenCommandLineModeCombo.getComponent()
            );
            builder.addLabeled(TestNGLocalize.testngConfigurationPropertiesFile(), myPropertiesFileField.getComponent());
            builder.setBottom(createTestSettingsTabs());
        }

        @RequiredUIAccess
        private Component createTestSettingsTabs() {
            ToolbarDecoratorBuilderFactory decoratorFactory = myProject.getApplication().getInstance(ToolbarDecoratorBuilderFactory.class);

            Component propertiesPanel = decoratorFactory.create(myPropertiesTable)
                .addOrReplaceAction(new AddAction<>() {
                    @Override
                    @RequiredUIAccess
                    protected void doAdd(AnActionEvent e) {
                        PropertyRow row = new PropertyRow("", "");
                        myPropertiesModel.add(row);
                        myPropertiesTable.select(row);
                    }
                })
                .disableAction(EditAction.class)
                .disableAction(UpMoveAction.class)
                .disableAction(DownMoveAction.class)
                .build();

            Component listenersDecorator = decoratorFactory.create(myListenersList)
                .addOrReplaceAction(new AddListenerAction())
                .disableAction(EditAction.class)
                .disableAction(UpMoveAction.class)
                .disableAction(DownMoveAction.class)
                .build();

            DockLayout listenersPanel = DockLayout.create();
            listenersPanel.center(listenersDecorator);
            listenersPanel.bottom(myUseDefaultReportersBox);

            TabbedLayout tabs = TabbedLayout.create();
            addTab(tabs, TestNGLocalize.testngConfigurationParametersPane(), propertiesPanel);
            addTab(tabs, TestNGLocalize.testngConfigurationListenersPane(), listenersPanel);
            return tabs;
        }

        @RequiredUIAccess
        private void initialize() {
            myModel.setTypeField(TestType.PACKAGE, myPackageField);
            myModel.setTypeField(TestType.CLASS, myClassField);
            myModel.setTypeField(TestType.METHOD, myMethodField);
            myModel.setTypeField(TestType.GROUP, myGroupField);
            myModel.setTypeField(TestType.SUITE, mySuiteField.getComponent());
            myModel.setTypeField(TestType.PATTERN, myPatternField);
            myModel.setPropertiesFileField(myPropertiesFileField.getComponent());
            myModel.setOutputDirectoryField(myOutputDirectoryField.getComponent());
            myModel.setListener(TestNGConfigurationEditor.this);

            myTypeChooser.addValueListener(event -> {
                TestType type = event.getValue();
                if (type != null) {
                    myModel.setType(type);
                }
            });
            myScopeGroup.addValueListener(scope -> evaluateModuleClassPath());

            myModel.setType(TestType.CLASS);
        }

        @RequiredUIAccess
        private void onTypeChanged(TestType type) {
            myTypeChooser.setValue(type, false);

            boolean method = type == TestType.METHOD || type == TestType.SOURCE;
            showRow(myTestLocations.get(TestType.PACKAGE), type == TestType.PACKAGE);
            showRow(myTestLocations.get(TestType.CLASS), type == TestType.CLASS || method);
            showRow(myTestLocations.get(TestType.METHOD), method);
            showRow(myTestLocations.get(TestType.GROUP), type == TestType.GROUP);
            showRow(myTestLocations.get(TestType.SUITE), type == TestType.SUITE);
            showRow(myTestLocations.get(TestType.PATTERN), type == TestType.PATTERN);
            showRow(myScopesRow, isScopeShown(type));

            evaluateModuleClassPath();
        }

        @RequiredUIAccess
        private void showRow(@Nullable Row row, boolean shown) {
            if (row != null) {
                row.setVisible(shown);
                row.setEnabled(shown);
            }
        }

        @RequiredUIAccess
        private void evaluateModuleClassPath() {
            boolean allPackagesInProject = isScopeShown(myModel.getType()) && myScopeGroup.getValue() == TestSearchScope.WHOLE_PROJECT;
            Row moduleRow = myModuleRow;
            if (moduleRow != null) {
                moduleRow.setEnabled(!allPackagesInProject);
            }
            if (allPackagesInProject) {
                myModuleSelector.setSelectedModule(null);
            }
        }

        @Override
        @RequiredUIAccess
        public void apply(T configuration) {
            myModel.apply(configuration);
            myModuleSelector.applyTo(configuration);

            TestData data = configuration.getPersistantData();
            TestType type = myModel.getType();
            if (type != TestType.CLASS && type != TestType.METHOD && type != TestType.SOURCE) {
                TestSearchScope scope = myScopeGroup.getValue();
                data.setScope(scope == null ? TestSearchScope.WHOLE_PROJECT : scope);
            }
            else {
                data.setScope(TestSearchScope.MODULE_WITH_DEPENDENCIES);
            }

            super.apply(configuration);

            configuration.setAlternativeJrePath(myJrePathEditor.getJrePathOrName());
            configuration.setAlternativeJrePathEnabled(myJrePathEditor.isAlternativeJreSelected());

            data.TEST_PROPERTIES.clear();
            for (PropertyRow row : myPropertiesModel) {
                data.TEST_PROPERTIES.put(row.getName(), row.getValue());
            }

            data.TEST_LISTENERS.clear();
            for (String listener : myListenersModel) {
                data.TEST_LISTENERS.add(listener);
            }

            data.USE_DEFAULT_REPORTERS = Boolean.TRUE.equals(myUseDefaultReportersBox.getValue());
            configuration.setShortenCommandLine(myShortenCommandLineModeCombo.getSelectedItem());
        }

        @Override
        @RequiredUIAccess
        public void reset(T configuration) {
            super.reset(configuration);

            myModuleSelector.reset(configuration);
            setModuleContext(myModuleSelector.getModule());

            TestData data = configuration.getPersistantData();
            TestSearchScope scope = data.getScope();
            myScopeGroup.setValue(
                scope == TestSearchScope.SINGLE_MODULE || scope == TestSearchScope.MODULE_WITH_DEPENDENCIES
                    ? scope
                    : TestSearchScope.WHOLE_PROJECT,
                false
            );

            myModel.reset(configuration);

            myJrePathEditor.setByName(configuration.isAlternativeJrePathEnabled() ? configuration.getAlternativeJrePath() : null);

            List<PropertyRow> properties = new ArrayList<>(data.TEST_PROPERTIES.size());
            for (Map.Entry<String, String> entry : data.TEST_PROPERTIES.entrySet()) {
                properties.add(new PropertyRow(StringUtil.notNullize(entry.getKey()), StringUtil.notNullize(entry.getValue())));
            }
            myPropertiesModel.replaceAll(properties);
            myListenersModel.replaceAll(data.TEST_LISTENERS);

            myUseDefaultReportersBox.setValue(data.USE_DEFAULT_REPORTERS);
            myShortenCommandLineModeCombo.setSelectedItem(configuration.getShortenCommandLine());
        }

        private final class AddListenerAction extends AddAction<String> implements AnActionWithSyncUpdate {
            @Override
            public void update(AnActionEvent e) {
                e.getPresentation().setEnabled(!myProject.isDefault());
            }

            @Override
            @RequiredUIAccess
            protected void doAdd(AnActionEvent e) {
                addListener();
            }
        }
    }
}
