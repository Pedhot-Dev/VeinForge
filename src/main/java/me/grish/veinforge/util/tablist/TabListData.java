package me.grish.veinforge.util.tablist;

import me.grish.veinforge.VeinForgeClient;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TabListData {

    public static final TabListData EMPTY = new TabListData();
    public Set<WidgetType> activeWidgets;
    public Map<WidgetType, List<String>> widgetLines = new EnumMap<>(
            WidgetType.class
    );

    public String serialize() {
        return VeinForgeClient.GSON.toJson(this);
    }
}
