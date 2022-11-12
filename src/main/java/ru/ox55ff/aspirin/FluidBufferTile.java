package ru.ox55ff.aspirin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class FluidBufferTile extends TileEntity implements IFluidHandler {
    private static final int TankCapacity = 16_000;
    private static final int MinOut = 2_000;
    private static final String TankCountKey = "tankCount";
    private static final String TankTagPrefixKey = "tank_";

    private ArrayList<FluidTank> _tanks;
    private IFluidHandler _target = null;
    private boolean _neighborInitialized = false;

    public FluidBufferTile() {
        _tanks = createTanks(9);
    }

    private ArrayList<FluidTank> createTanks(int tankCount) {
        ArrayList<FluidTank> tanks = new ArrayList<>(tankCount);
        for (int i = 0; i < tankCount; i++) {
            tanks.add(new FluidTank(TankCapacity));
        }

        return tanks;
    }

    private FluidTank getTankByFluidStack(FluidStack stack) {
        for (FluidTank tank : _tanks) {
            if (stack.isFluidEqual(tank.getFluid())) {
                return tank;
            }
        }

        return null;
    }

    public ForgeDirection getSide() {
        return ForgeDirection.getOrientation(getBlockMetadata());
    }

    public void onActivated(EntityPlayer player) {
        FluidTankInfo[] info = getTankInfo(ForgeDirection.UNKNOWN);
        boolean empty = true;

        for (FluidTankInfo item : info) {
            if (item.fluid != null && item.fluid.amount > 0) {
                player.addChatMessage(new ChatComponentText(
                    String.format("%s: %d", FluidRegistry.getFluidName(item.fluid), item.fluid.amount)));
                empty = false;
            }
        }

        if (empty) {
            player.addChatMessage(new ChatComponentText("Empty"));
        }

//        System.out.println("target: " + (_target != null));
    }

    public void updateNeighbors() {
        _neighborInitialized = false;
    }

    public void onNeighborChange(int tileX, int tileY, int tileZ) {
        if (worldObj.isRemote) {
            return;
        }

        ForgeDirection side = getSide();
        int targetX = xCoord + side.offsetX;
        int targetY = yCoord + side.offsetY;
        int targetZ = zCoord + side.offsetZ;

        if (targetX == tileX && targetY == tileY && targetZ == tileZ) {
            TileEntity tile = worldObj.getTileEntity(targetX, targetY, targetZ);
            if (tile instanceof IFluidHandler) {
                _target = (IFluidHandler) tile;
            } else {
                _target = null;
            }
        }
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (worldObj.isRemote || from == getSide()) {
            return 0;
        }

        int res = 0;
        FluidTank notEmptyTank = getTankByFluidStack(resource);
        if (notEmptyTank != null) {
            res = notEmptyTank.fill(resource, doFill);
        } else {
            for (FluidTank tank : _tanks) {
                if (tank.getFluidAmount() == 0) {
                    res = tank.fill(resource, doFill);
                    break;
                }
            }
        }

        this.markDirty();
        return res;
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        return null;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return null;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return from != getSide();
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return false;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return _tanks.stream().map(FluidTank::getInfo).toArray(FluidTankInfo[]::new);
    }

    @Override
    public void updateEntity() {
        if (!worldObj.isRemote && !_neighborInitialized) {
            checkNeighbor();
            _neighborInitialized = true;
        }

        if (!worldObj.isRemote && worldObj.getTotalWorldTime() % 20 == 0 && _target != null) {
            ForgeDirection side = getSide();
            for (FluidTank tank : _tanks) {
                if (tank.getFluidAmount() >= MinOut) {
                    FluidStack fluidStack = tank.drain(MinOut, false);
                    if (fluidStack != null && fluidStack.amount == MinOut) {
                        int filled = _target.fill(side.getOpposite(), fluidStack, false);
                        if (filled == MinOut) {
                            fluidStack = tank.drain(MinOut, true);
                            _target.fill(side.getOpposite(), fluidStack, true);
                            this.markDirty();
                            return;
                        }
                    }
                }
            }
        }
    }

    private void checkNeighbor() {
        onNeighborChange(xCoord + 1, yCoord, zCoord);
        onNeighborChange(xCoord - 1, yCoord, zCoord);

        onNeighborChange(xCoord, yCoord + 1, zCoord);
        onNeighborChange(xCoord, yCoord - 1, zCoord);

        onNeighborChange(xCoord, yCoord, zCoord + 1);
        onNeighborChange(xCoord, yCoord, zCoord - 1);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        readExtendedData(nbt);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        writeExtendedData(nbt);
    }

    private void writeExtendedData(NBTTagCompound nbt) {
        nbt.setInteger(TankCountKey, _tanks.size());

        for (int i = 0; i < _tanks.size(); i++) {
            FluidTank tank = _tanks.get(i);

            NBTTagCompound tankNbt = new NBTTagCompound();
            tank.writeToNBT(tankNbt);

            nbt.setTag(TankTagPrefixKey + i, tankNbt);
        }
    }

    @Nullable
    private void readExtendedData(NBTTagCompound nbt) {
        if (!nbt.hasKey(TankCountKey, Constants.NBT.TAG_INT)) {
            return;
        }

        int tankCount = nbt.getInteger(TankCountKey);
        _tanks = createTanks(tankCount);

        for (int i = 0; i < _tanks.size(); i++) {
            String key = TankTagPrefixKey + i;
            if (!nbt.hasKey(key, Constants.NBT.TAG_COMPOUND)) {
                return;
            }

            NBTTagCompound tankNbt = nbt.getCompoundTag(key);
            _tanks.get(i).readFromNBT(tankNbt);
        }
    }

//    @Override
//    public Packet getDescriptionPacket() {
//        NBTTagCompound tagCompound = new NBTTagCompound();
//        this.writeToNBT(tagCompound);
//
//        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 3, tagCompound);
//    }
//
//    @Override
//    public void onDataPacket(NetworkManager networkManager, S35PacketUpdateTileEntity packet) {
//        NBTTagCompound tagCompound = packet.func_148857_g();
//        this.readFromNBT(tagCompound);
//    }
}
