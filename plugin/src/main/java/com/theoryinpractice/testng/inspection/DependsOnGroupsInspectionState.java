package com.theoryinpractice.testng.inspection;

import consulo.configurable.ConfigurableBuilder;
import consulo.configurable.ConfigurableBuilderState;
import consulo.configurable.UnnamedConfigurable;
import consulo.language.editor.inspection.InspectionToolState;
import consulo.localize.LocalizeValue;
import consulo.ui.Label;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.XmlSerializerUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 27/03/2023
 */
public class DependsOnGroupsInspectionState implements InspectionToolState<DependsOnGroupsInspectionState>
{
	public List<String> groups = new ArrayList<>();

	@Nullable
	@Override
	public UnnamedConfigurable createConfigurable()
	{
		ConfigurableBuilder<ConfigurableBuilderState> builder = ConfigurableBuilder.newBuilder();
		builder.component(() -> Label.create(LocalizeValue.localizeTODO("Defined Groups:")));
		builder.textBoxWithExpandAction(null, "Edit Groups", s -> StringUtil.split(s, ","), strings -> String.join(",", strings), () -> {
			return String.join(",", groups);
		}, s -> {
			groups.clear();
			for(String group : StringUtil.split(s, ","))
			{
				groups.add(group);
			}
		});
		return builder.buildUnnamed();
	}

	@Nullable
	@Override
	public DependsOnGroupsInspectionState getState()
	{
		return this;
	}

	@Override
	public void loadState(DependsOnGroupsInspectionState state)
	{
		XmlSerializerUtil.copyBean(state, this);
	}
}
