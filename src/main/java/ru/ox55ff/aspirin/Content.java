package ru.ox55ff.aspirin;

import cpw.mods.fml.common.registry.GameRegistry;

public class Content {
    public static final FluidBuffer FLUIDBUFFER = new FluidBuffer();

    public static void register() {
        GameRegistry.registerBlock(FLUIDBUFFER, "fluidBuffer");

        GameRegistry.registerTileEntity(FluidBufferTile.class, Tags.MODID + "fluidBuffer");
    }
}
