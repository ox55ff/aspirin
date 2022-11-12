package ru.ox55ff.aspirin;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.enums.ItemList;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class Content {
    public static final FluidBuffer FLUIDBUFFER = new FluidBuffer();

    public static void register() {
        GameRegistry.registerBlock(FLUIDBUFFER, "fluidBuffer");
        GameRegistry.registerTileEntity(FluidBufferTile.class, Tags.MODID + "fluidBuffer");
    }

    public static void registerRecipes() {
        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(FLUIDBUFFER),
            " C ", "PBP", "   ",
            'C', "circuitGood",
            'P', "pipeMediumSteel",
            'B', ItemList.Casing_LV.get(1)));
    }
}
