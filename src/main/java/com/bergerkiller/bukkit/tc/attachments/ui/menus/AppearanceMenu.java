package com.bergerkiller.bukkit.tc.attachments.ui.menus;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.events.map.MapKeyEvent;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapEventPropagation;
import com.bergerkiller.bukkit.common.map.MapPlayerInput.Key;
import com.bergerkiller.bukkit.common.map.widgets.MapWidget;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetButton;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetTabView;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetWindow;
import com.bergerkiller.bukkit.common.resources.CommonSounds;
import com.bergerkiller.bukkit.common.utils.ParseUtil;
import com.bergerkiller.bukkit.tc.attachments.config.CartAttachmentType;
import com.bergerkiller.bukkit.tc.attachments.ui.ItemDropTarget;
import com.bergerkiller.bukkit.tc.attachments.ui.MapWidgetAttachmentNode;
import com.bergerkiller.bukkit.tc.attachments.ui.MapWidgetSelectionBox;
import com.bergerkiller.bukkit.tc.attachments.ui.entity.MapWidgetEntityTypeList;
import com.bergerkiller.bukkit.tc.attachments.ui.item.MapWidgetItemSelector;

public class AppearanceMenu extends MapWidgetWindow implements ItemDropTarget {
    private final MapWidgetAttachmentNode attachment;
    private final MapWidgetTabView tabView = new MapWidgetTabView();

    public AppearanceMenu(MapWidgetAttachmentNode attachment) {
        this.attachment = attachment;
        this.setBounds(5, 15, 118, 104);
        this.setDepthOffset(4);
        this.setFocusable(true);
        this.setBackgroundColor(MapColorPalette.COLOR_BLUE);
    }

    @Override
    public void onAttached() {
        //this.activate();

        // Tab view widget to switch between different appearance editing modes
        // The order of these tabs is important, and must match the order in CartAttachmentType!
        tabView.addTab(); // EMPTY
        tabView.addTab().addWidget(new MapWidgetEntityTypeList() { // ENTITY
            @Override
            public void onAttached() {
                super.onAttached();
                this.setEntityType(getAttachment().getConfig().get("entityType", EntityType.MINECART));
            }

            @Override
            public void onEntityTypeChanged() {
                getAttachment().getConfig().set("entityType", this.getEntityType());
                markChanged();
            }
        }).setBounds(0, 0, 100, 11);
        tabView.addTab().addWidget(new MapWidgetItemSelector() { // ITEM
            @Override
            public void onAttached() {
                super.onAttached();
                this.setSelectedItem(getAttachment().getConfig().get("item", new ItemStack(Material.PUMPKIN)));
            }

            @Override
            public void onSelectedItemChanged() {
                getAttachment().getConfig().set("item", this.getSelectedItem());
                markChanged();
            }
        });
        tabView.addTab().addWidget(new MapWidgetButton() { // SEAT
            private boolean checked = false;

            @Override
            public void onAttached() {
                super.onAttached();
                this.checked = getAttachment().getConfig().get("lockRotation", false);
                updateText();
            }

            private void updateText() {
                this.setText("Lock Rotation: " + (checked ? "ON":"OFF"));
            }

            @Override
            public void onActivate() {
                this.checked = !this.checked;
                updateText();
                getAttachment().getConfig().set("lockRotation", this.checked);
                markChanged();
                display.playSound(CommonSounds.CLICK);
            }
        }).setBounds(0, 10, 100, 16);
        tabView.addTab(); // MODEL

        tabView.setPosition(7, 16);
        this.addWidget(tabView);

        // This widget is always visible: type selector
        MapWidgetSelectionBox typeSelectionBox = this.addWidget(new MapWidgetSelectionBox() {
            @Override
            public void onSelectedItemChanged() {
                setType(ParseUtil.parseEnum(this.getSelectedItem(), CartAttachmentType.EMPTY));
            }
        });
        for (CartAttachmentType type : CartAttachmentType.values()) {
            typeSelectionBox.addItem(type.toString());
        }
        typeSelectionBox.setSelectedItem(getAttachment().getConfig().get("type", String.class));
        typeSelectionBox.setBounds(7, 3, 100, 11);

        // Set to display currently selected type
        setType(getAttachment().getConfig().get("type", CartAttachmentType.EMPTY));

        this.tabView.activate();
    }

    public void setType(CartAttachmentType type) {
        if (getAttachment().getConfig().get("type", CartAttachmentType.EMPTY) != type) {
            getAttachment().getConfig().set("type", type);
            markChanged();
        }

        // Switch tab
        this.tabView.setSelectedIndex(type.ordinal());
    }

    @Override
    public void onKeyPressed(MapKeyEvent event) {
        if (event.getKey() == Key.BACK && this.isActivated()) {
            this.removeWidget();
            return;
        }
        super.onKeyPressed(event);
    }

    @Override
    public boolean acceptItem(ItemStack item) {
        for (MapWidget widget : this.tabView.getSelectedTab().getWidgets()) {
            if (widget instanceof ItemDropTarget && ((ItemDropTarget) widget).acceptItem(item)) {
                return true;
            }
        }
        return false;
    }

    public ConfigurationNode getConfig() {
        return this.attachment.getConfig();
    }

    public MapWidgetAttachmentNode getAttachment() {
        return this.attachment;
    }

    private void markChanged() {
        sendStatusChange(MapEventPropagation.DOWNSTREAM, "changed");
        getAttachment().resetIcon();
    }

}
