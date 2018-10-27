package com.bergerkiller.bukkit.tc.attachments.ui.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.events.map.MapKeyEvent;
import com.bergerkiller.bukkit.common.map.MapColorPalette;
import com.bergerkiller.bukkit.common.map.MapFont;
import com.bergerkiller.bukkit.common.map.MapTexture;
import com.bergerkiller.bukkit.common.map.MapPlayerInput.Key;
import com.bergerkiller.bukkit.common.map.widgets.MapWidget;
import com.bergerkiller.bukkit.common.nbt.CommonTagCompound;
import com.bergerkiller.bukkit.common.resources.CommonSounds;
import com.bergerkiller.bukkit.common.utils.ItemUtil;
import com.bergerkiller.bukkit.tc.TCConfig;
import com.bergerkiller.bukkit.tc.TrainCarts;

/**
 * Interactive widget that pops down a full list of item base types when
 * activated, and allows switching between item/block variants using left/right.
 */
public abstract class MapWidgetItemVariantList extends MapWidget {
    private final MapTexture background;
    private List<ItemStack> variants;
    private Map<ItemStack, MapTexture> iconCache = new HashMap<ItemStack, MapTexture>();
    private int variantIndex = 0;

    public MapWidgetItemVariantList() {
        this.background = MapTexture.loadPluginResource(TrainCarts.plugin, "com/bergerkiller/bukkit/tc/textures/attachments/item_selector_bg.png");
        this.setSize(86, 18);
        this.setFocusable(true);
        this.variants = new ArrayList<ItemStack>(0);
    }

    public ItemStack getItem() {
        if (this.variantIndex >= 0 && this.variantIndex < this.variants.size()) {
            return this.variants.get(this.variantIndex);
        } else {
            return null;
        }
    }

    public void setItem(ItemStack item) {
        if (item == null) {
            this.variants = new ArrayList<ItemStack>(0);
            this.variantIndex = 0;
            this.invalidate();
            this.onItemChanged();
            return;
        }
        int maxDurability = ItemUtil.getMaxDurability(item);
        if (maxDurability > 0) {
            // Uses durability
            this.variants = new ArrayList<ItemStack>(maxDurability);
            for (int i = 0; i <= maxDurability; i++) {
                ItemStack tmp = ItemUtil.createItem(item.clone());
                tmp.setDurability((short) i);
                this.variants.add(tmp);
            }
        } else {
            // Find variants using internal lookup (creative menu)
            this.variants = ItemUtil.getItemVariants(item.getType());

            // Guarantee CraftItemStack
            for (int i = 0; i < this.variants.size(); i++) {
                this.variants.set(i, ItemUtil.createItem(this.variants.get(i)));
            }

            // Preserve some of the extra properties of the input item
            for (ItemStack variant : this.variants) {
                for (Map.Entry<Enchantment, Integer> enchantment : item.getEnchantments().entrySet()) {
                    variant.addEnchantment(enchantment.getKey(), enchantment.getValue().intValue());
                }
            }
            if (item.getItemMeta().hasDisplayName()) {
                String name = item.getItemMeta().getDisplayName();
                for (ItemStack variant : this.variants) {
                    ItemUtil.setDisplayName(variant, name);
                }
            }
            CommonTagCompound tag = ItemUtil.getMetaTag(item);
            if (tag != null && tag.containsKey("Unbreakable") && tag.getValue("Unbreakable", false)) {
                for (ItemStack variant : this.variants) {
                    ItemUtil.getMetaTag(variant, true).putValue("Unbreakable", true);
                }
            }
        }

        // Find the item in the variants to deduce the currently selected index
        this.variantIndex = 0;
        for (int i = 0; i < this.variants.size(); i++) {
            ItemStack variant = this.variants.get(i);
            if (variant.isSimilar(item)) {
                this.variantIndex = i;
                break; // Final!
            }
            if (variant.getDurability() == item.getDurability()) {
                this.variantIndex = i;
            }
        }

        this.invalidate();
        this.onItemChanged();
    }

    @Override
    public void onDraw() {
        // Background
        this.view.draw(this.background, 0, 0);

        // Draw the same item with -2 to +2 variant indices
        int x = 1;
        int y = 1;
        for (int index = this.variantIndex - 2; index <= this.variantIndex + 2; index++) {
            // Check index valid
            if (index >= 0 && index < this.variants.size()) {
                ItemStack item = this.variants.get(index);
                MapTexture icon = this.iconCache.get(item);
                if (icon == null) {
                    icon = MapTexture.createEmpty(16, 16);
                    icon.fillItem(TCConfig.resourcePack, item);
                    this.iconCache.put(item, icon);
                }
                view.draw(icon, x, y);
            }
            x += 17;
        }

        // If variants are based on durability, show durability value
        if (this.variantIndex >= 0 && this.variantIndex < this.variants.size()) {
            ItemStack item = this.variants.get(this.variantIndex);
            if (ItemUtil.getMaxDurability(item) > 0) {
                view.setAlignment(MapFont.Alignment.MIDDLE);
                view.draw(MapFont.TINY, 44, 12, MapColorPalette.COLOR_RED, Short.toString(item.getDurability()));
            }
        }
    }

    private void changeVariantIndex(int offset) {
        int newVariantIndex = this.variantIndex + offset;
        if (newVariantIndex < 0) {
            newVariantIndex = 0;
        } else if (newVariantIndex >= this.variants.size()) {
            newVariantIndex = this.variants.size()-1;
        }
        if (this.variantIndex == newVariantIndex) {
            return;
        }
        this.variantIndex = newVariantIndex;
        this.invalidate();
        this.onItemChanged();
        this.display.playSound(CommonSounds.CLICK);
    }

    @Override
    public void onKeyPressed(MapKeyEvent event) {
        if (event.getKey() == Key.LEFT) {
            changeVariantIndex(-1 - (event.getRepeat() / 40));
        } else if (event.getKey() == Key.RIGHT) {
            changeVariantIndex(1 + (event.getRepeat() / 40));
        } else {
            super.onKeyPressed(event);
        }
    }

    public abstract void onItemChanged();
}
