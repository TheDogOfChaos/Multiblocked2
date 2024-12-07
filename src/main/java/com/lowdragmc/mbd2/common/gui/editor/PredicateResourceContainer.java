package com.lowdragmc.mbd2.common.gui.editor;

import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.gui.editor.ui.ConfigPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.editor.ui.ResourcePanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.mbd2.api.pattern.predicates.SimplePredicate;
import com.lowdragmc.mbd2.common.gui.editor.multiblock.MultiblockPatternPanel;
import com.lowdragmc.mbd2.integration.ldlib.MBDLDLibPlugin;
import net.minecraft.client.Minecraft;

public class PredicateResourceContainer extends ResourceContainer<SimplePredicate, Widget> {

    public PredicateResourceContainer(Resource<SimplePredicate> resource, ResourcePanel panel) {
        super(resource, panel);
        setWidgetSupplier(k -> createPreview(getResource().getResource(k)));
        setDragging(key -> key, (k, o, p) -> getResource().getResource(k).getPreviewTexture());
        setOnEdit(key -> {
            var predicate = getResource().getResource(key);
            if (predicate == SimplePredicate.ANY || predicate == SimplePredicate.AIR) return;
            getPanel().getEditor().getConfigPanel().openConfigurator(ConfigPanel.Tab.RESOURCE, predicate);
        });
        setOnRemove(key -> {
            if (key.equals("any") || key.equals("air")) return false;
            if (Editor.INSTANCE.getCurrentProject() instanceof MultiblockMachineProject project) {
                boolean changed = false;
                for (var x : project.getBlockPlaceholders()) {
                    for (var y : x) {
                        for (var z : y) {
                            changed |= z.getPredicates().remove(key);
                        }
                    }
                }
                if (changed) {
                    Editor.INSTANCE.getTabPages().tabs.values().stream()
                            .filter(MultiblockPatternPanel.class::isInstance)
                            .map(MultiblockPatternPanel.class::cast)
                            .findAny().ifPresent(MultiblockPatternPanel::onBlockPlaceholdersChanged);
                }
            }
            return true;
        });
    }

    protected ImageWidget createPreview(SimplePredicate predicate) {
        return new ImageWidget(0, 0, 33, 33, predicate::getPreviewTexture);
    }

    @Override
    protected void renameResource() {
        if (selected != null) {
            DialogWidget.showStringEditorDialog(Editor.INSTANCE, LocalizationUtils.format("ldlib.gui.editor.tips.rename") + " " + LocalizationUtils.format(resource.name()), selected, s -> {
                if (resource.hasResource(s)) {
                    return false;
                }
                if (renamePredicate != null) {
                    return renamePredicate.test(s);
                }
                return true;
            }, s -> {
                if (s == null || selected.equals(s)) return;
                var stored =  resource.removeResource(selected);
                resource.addResource(s, stored);
                var prevName = selected; // selected is set to null on rebuild
                reBuild();
                if (Editor.INSTANCE.getCurrentProject() instanceof MultiblockMachineProject project) {
                    boolean changed = false;
                    for (var x : project.getBlockPlaceholders()) {
                        for (var y : x) {
                            for (var z : y) {
                                if (z.getPredicates().remove(prevName)) {
                                    z.getPredicates().add(s);
                                    changed = true;
                                }
                            }
                        }
                    }
                    if (changed) {
                        Editor.INSTANCE.getTabPages().tabs.values().stream()
                                .filter(MultiblockPatternPanel.class::isInstance)
                                .map(MultiblockPatternPanel.class::cast)
                                .findAny().ifPresent(MultiblockPatternPanel::onBlockPlaceholdersChanged);
                    }
                }
            });
        }
    }

    @Override
    protected TreeBuilder.Menu getMenu() {
        return TreeBuilder.Menu.start()
                .leaf(Icons.EDIT_FILE, "ldlib.gui.editor.menu.edit", this::editResource)
                .leaf("ldlib.gui.editor.menu.rename", this::renameResource)
                .crossLine()
                .leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", this::copy)
                .leaf(Icons.PASTE, "ldlib.gui.editor.menu.paste", this::paste)
                .branch(Icons.ADD_FILE, "config.predicate.add_predicate", menu -> {
                    for (var entry : MBDLDLibPlugin.REGISTER_PREDICATES.entrySet()) {
                        menu.leaf("config.predicate.%s".formatted(entry.getKey()), () -> {
                            var predicate = entry.getValue().creator().get().buildPredicate();
                            predicate.buildPredicate();
                            resource.addResource(genNewFileName(), predicate);
                            reBuild();
                        });
                    }
                })
                .leaf(Icons.REMOVE_FILE, "ldlib.gui.editor.menu.remove", this::removeSelectedResource)
                .leaf(Icons.ROTATION, "ldlib.gui.editor.menu.reload_resource", () -> Minecraft.getInstance().reloadResourcePacks());
    }
}
