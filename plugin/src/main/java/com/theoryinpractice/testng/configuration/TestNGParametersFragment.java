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
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.testng.localize.TestNGLocalize;
import consulo.ui.Component;
import consulo.ui.Table;
import consulo.ui.TableItemEditor;
import consulo.ui.TextBox;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Suite parameters passed to TestNG, shown while there are any.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
class TestNGParametersFragment implements RunConfigurationFragment<TestNGConfiguration> {
    @Override
    public String getId() {
        return "testng.parameters";
    }

    @Override
    public LocalizeValue getName() {
        return TestNGLocalize.runConfigurationFragmentParameters();
    }

    @Override
    public LocalizeValue getGroup() {
        return TestNGLocalize.runConfigurationFragmentGroupTestng();
    }

    @Override
    public boolean isVisibleByDefault(TestNGConfiguration target) {
        return !target.getOptions().getProperties().isEmpty();
    }

    @Override
    public void applyHidden(TestNGConfiguration target) {
        target.getOptions().setProperties(Map.of());
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<TestNGConfiguration> createEditor(Project project, Disposable parent) {
        MutableFlatDataModel<PropertyRow> model = FlatDataModel.of(new ArrayList<>());
        Table<PropertyRow> table = createTable(model);

        Component panel = project.getApplication().getInstance(ToolbarDecoratorBuilderFactory.class)
            .create(table)
            .addOrReplaceAction(new AddAction<>() {
                @Override
                @RequiredUIAccess
                protected void doAdd(AnActionEvent e) {
                    PropertyRow row = new PropertyRow("", "");
                    model.add(row);
                    table.select(row);
                }
            })
            .disableAction(EditAction.class)
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class)
            .build();
        LabeledLayout layoutComponent = LabeledLayout.create(TestNGLocalize.testngConfigurationParametersPane(), panel);

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addBottom(layoutComponent);
            }

            @RequiredUIAccess
            @Override
            public void reset(TestNGConfiguration target) {
                Map<String, String> properties = target.getOptions().getProperties();
                List<PropertyRow> rows = new ArrayList<>(properties.size());
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    rows.add(new PropertyRow(StringUtil.notNullize(entry.getKey()), StringUtil.notNullize(entry.getValue())));
                }
                model.replaceAll(rows);
            }

            @RequiredUIAccess
            @Override
            public void apply(TestNGConfiguration target) {
                Map<String, String> properties = new LinkedHashMap<>();
                for (PropertyRow row : model) {
                    properties.put(row.getName(), row.getValue());
                }
                target.getOptions().setProperties(properties);
            }
        };
    }

    @RequiredUIAccess
    private static Table<PropertyRow> createTable(MutableFlatDataModel<PropertyRow> model) {
        Table<PropertyRow> table = Table.create(model);
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
}
