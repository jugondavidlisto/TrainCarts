package com.bergerkiller.bukkit.tc.attachments.ui.menus;

import org.bukkit.inventory.ItemStack;

import static com.bergerkiller.bukkit.common.utils.MaterialUtil.getMaterial;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetButton;
import com.bergerkiller.bukkit.common.map.widgets.MapWidgetWindow;
import com.bergerkiller.bukkit.tc.attachments.config.CartAttachmentType;
import com.bergerkiller.bukkit.tc.attachments.ui.MapWidgetAttachmentNode;

public class GeneralMenu extends MapWidgetWindow {
    private final MapWidgetAttachmentNode attachment;

    public GeneralMenu(MapWidgetAttachmentNode attachment) {
        this.attachment = attachment;
        this.setBounds(5, 15, 118, 104);
        this.setDepthOffset(4);
        this.setFocusable(true);
        this.setBackgroundColor(MapColorPalette.COLOR_YELLOW);
    }

    @Override
    public void onAttached() {
        this.activate();

        this.addWidget(new MapWidgetButton() {
            @Override
            public void onActivate() {
                ConfigurationNode config = new ConfigurationNode();
                config.set("type", CartAttachmentType.ITEM);
                config.set("item", new ItemStack(getMaterial("LEGACY_WOOD")));
                attachment.addAttachment(config);
                GeneralMenu.this.deactivate();
            }
        }).setText("Add Attachment").setBounds(10, 10, 98, 18);

        this.addWidget(new MapWidgetButton() {
            @Override
            public void onActivate() {
                attachment.setChangingOrder(true);
                GeneralMenu.this.deactivate();
            }
        }).setText("Change order").setBounds(10, 30, 98, 18);

        this.addWidget(new MapWidgetButton() {
            @Override
            public void onActivate() {
                attachment.remove();
                GeneralMenu.this.deactivate();
            }
        }).setText("Delete").setBounds(10, 50, 98, 18).setEnabled(attachment.getParentAttachment() != null);

        this.addWidget(new MapWidgetButton() {
            @Override
            public void onActivate() {
                int index = attachment.getParentAttachment().getAttachments().indexOf(attachment);
                MapWidgetAttachmentNode addedNode;
                addedNode = attachment.getParentAttachment().addAttachment(index+1, attachment.getFullConfig());
                attachment.getTree().setSelectedNode(addedNode);
                GeneralMenu.this.deactivate();
            }
        }).setText("Duplicate").setBounds(10, 70, 98, 18).setEnabled(attachment.getParentAttachment() != null);
    }

    @Override
    public void onDeactivate() {
        this.removeWidget();
    }

    public ConfigurationNode getConfig() {
        return this.attachment.getConfig().getNode("position");
    }

    public MapWidgetAttachmentNode getAttachment() {
        return this.attachment;
    }

}
