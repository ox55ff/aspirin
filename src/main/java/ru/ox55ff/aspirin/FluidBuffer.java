package ru.ox55ff.aspirin;

import buildcraft.api.tools.IToolWrench;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;

public class FluidBuffer extends BlockContainer {
    private static final int[] DirMapping = {2, 5, 3, 4}; // NORTH, EAST, SOUTH, WEST

    @SideOnly(Side.CLIENT)
    private IIcon faceIcon;

    public FluidBuffer() {
        super(Material.iron);
        setBlockName("fluidBuffer");
        setBlockTextureName(Tags.MODID + ":fluidBuffer");
        setCreativeTab(CreativeTabs.tabMisc);
        setHardness(15F);
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

    @Override
    public void onNeighborChange(IBlockAccess world, int x, int y, int z, int tileX, int tileY, int tileZ) {
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
                ItemStack item = player.getHeldItem();

                if (item != null && item.getItem() instanceof IToolWrench) {
                    if (player.isSneaking()) {
                        Block block = world.getBlock(x, y, z);
                        if (block != null) {
                            world.setBlockToAir(x, y, z);
                            ArrayList<ItemStack> drops = block.getDrops(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
                            for (ItemStack i : drops) {
                                if (i != null && i.stackSize > 0) {
                                    final EntityItem ei = new EntityItem(world, x, y, z, i.copy());
                                    world.spawnEntityInWorld(ei);
                                    return false;
                                }
                            }
                        }
                    } else {
                        ForgeDirection clickDir = ForgeDirection.getOrientation(side);
                        world.setBlockMetadataWithNotify(x, y, z, clickDir.getOpposite().ordinal(), 3);
                        fbTile.updateNeighbors();
                        return true;
                    }
                }

                fbTile.onActivated(player);
                return true;
            }
        }

        return false;
    }

    @Override
    public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_) {
        return new FluidBufferTile();
    }
}
