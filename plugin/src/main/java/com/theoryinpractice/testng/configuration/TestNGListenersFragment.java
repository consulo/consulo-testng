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
import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.util.TreeClassChooser;
import com.intellij.java.language.util.TreeClassChooserFactory;
import com.theoryinpractice.testng.model.TestListenerFilter;
import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.project.Project;
import consulo.testng.localize.TestNGLocalize;
import consulo.ui.Component;
import consulo.ui.InputBoxBuilder;
import consulo.ui.ListBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * TestNG listener classes added to the run, shown while there are any.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
class TestNGListenersFragment implements RunConfigurationFragment<TestNGConfiguration> {
    @Override
    public String getId() {
        return "testng.listeners";
    }

    @Override
    public LocalizeValue getName() {
        return TestNGLocalize.runConfigurationFragmentListeners();
    }

    @Override
    public LocalizeValue getGroup() {
        return TestNGLocalize.runConfigurationFragmentGroupTestng();
    }

    @Override
    public boolean isVisibleByDefault(TestNGConfiguration target) {
        return !target.getOptions().getListeners().isEmpty();
    }

    @Override
    public void applyHidden(TestNGConfiguration target) {
        target.getOptions().setListeners(List.of());
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<TestNGConfiguration> createEditor(Project project, Disposable parent) {
        TestNGModuleHolder moduleHolder = new TestNGModuleHolder(project);
        MutableFlatDataModel<String> model = FlatDataModel.of(new ArrayList<>());
        ListBox<String> list = ListBox.create(model);

        Component panel = project.getApplication().getInstance(ToolbarDecoratorBuilderFactory.class)
            .create(list)
            .addOrReplaceAction(new AddListenerAction(project, moduleHolder, model, list))
            .disableAction(EditAction.class)
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class)
            .build();
        LabeledLayout layoutComponent = LabeledLayout.create(TestNGLocalize.testngConfigurationListenersPane(), panel);

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addBottom(layoutComponent);
            }

            @RequiredUIAccess
            @Override
            public void reset(TestNGConfiguration target) {
                moduleHolder.setModule(target.getConfigurationModule().getModule());
                model.replaceAll(target.getOptions().getListeners());
            }

            @RequiredUIAccess
            @Override
            public void apply(TestNGConfiguration target) {
                moduleHolder.setModule(target.getConfigurationModule().getModule());
                List<String> listeners = new ArrayList<>();
                for (String listener : model) {
                    listeners.add(listener);
                }
                target.getOptions().setListeners(listeners);
            }
        };
    }

    private static final class AddListenerAction extends AddAction<String> implements AnActionWithSyncUpdate {
        private final Project myProject;
        private final TestNGModuleHolder myModuleHolder;
        private final MutableFlatDataModel<String> myModel;
        private final ListBox<String> myList;

        private AddListenerAction(Project project, TestNGModuleHolder moduleHolder, MutableFlatDataModel<String> model, ListBox<String> list) {
            myProject = project;
            myModuleHolder = moduleHolder;
            myModel = model;
            myList = list;
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(!myProject.isDefault());
        }

        @Override
        @RequiredUIAccess
        protected void doAdd(AnActionEvent e) {
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
                .showAsync(myList)
                .whenCompleteAsync((value, error) -> {
                    if (error == null && !StringUtil.isEmptyOrSpaces(value)) {
                        appendListener(value.trim());
                    }
                }, uiAccess);
        }

        @RequiredUIAccess
        private @Nullable String selectListenerClass() {
            Module module = myModuleHolder.getModule();
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
            if (myModel.indexOf(className) < 0) {
                myModel.add(className);
            }
            myList.setValue(className);
        }
    }
}
