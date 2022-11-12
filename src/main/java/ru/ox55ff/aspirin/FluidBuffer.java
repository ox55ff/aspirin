package ru.ox55ff.aspirin;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class FluidBuffer extends BlockContainer {
    private static final int[] DirMapping = {2, 5, 3, 4}; // NORTH, EAST, SOUTH, WEST

    @SideOnly(Side.CLIENT)
    private IIcon faceIcon;

    public FluidBuffer() {
        super(Material.iron);
        setBlockName("fluidBuffer");
        setBlockTextureName(Tags.MODID + ":fluidBuffer");
        setCreativeTab(CreativeTabs.tabMisc);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        super.registerBlockIcons(register);
        faceIcon = register.registerIcon(Tags.MODID + ":fluidBufferOutput");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        ForgeDirection currentSideDir = ForgeDirection.getOrientation(side);
        ForgeDirection metaDir = ForgeDirection.getOrientation(meta);

        if (currentSideDir == metaDir) {
            return faceIcon;
        }

        return blockIcon;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        final int thresholdDeg = 35; // Отклонение от горизонта в градусах, чтобы считать, что смотрим вверх или вниз

        int direction;
        if (placer.rotationPitch < -thresholdDeg) { // Смотрим вверх
            direction = 0; // выход блока вниз
        } else if (placer.rotationPitch > thresholdDeg) { // смотрим вниз
            direction = 1; // выход блока вверх
        } else {
            // Выход блока в горизонтальную сторону, противоположную игроку
            int idx = MathHelper.floor_float(((placer.rotationYaw * 4.0f) / 360.0f) + 0.5f) & 3;
            direction = DirMapping[idx];
        }

        world.setBlockMetadataWithNotify(x, y, z, direction, 2);
    }

    /**
     * Called when a tile entity on a side of this block changes is created or is destroyed.
     * @param world The world
     * @param x The x position of this block instance
     * @param y The y position of this block instance
     * @param z The z position of this block instance
     * @param tileX The x position of the tile that changed
     * @param tileY The y position of the tile that changed
     * @param tileZ The z position of the tile that changed
     */
    @Override
    public void onNeighborChange(IBlockAccess world, int x, int y, int z, int tileX, int tileY, int tileZ)
    {
        System.out.printf("onNeighborChange: %d, %d, %d%n", x, y, z);
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof FluidBufferTile) {
            FluidBufferTile fbTile = (FluidBufferTile) tile;
            fbTile.onNeighborChange(tileX, tileY, tileZ);
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof FluidBufferTile) {
                FluidBufferTile fbTile = (FluidBufferTile) tile;
                fbTile.onActivated(player);
            }
        }

        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_) {
        return new FluidBufferTile();
    }
}
