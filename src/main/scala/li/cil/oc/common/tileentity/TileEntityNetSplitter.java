package li.cil.oc.common.tileentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.NodeContainer;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.network.AbstractTileEntityNodeContainer;
import li.cil.oc.common.capabilities.CapabilityEnvironment;
import li.cil.oc.common.tileentity.capabilities.RedstoneAwareImpl;
import li.cil.oc.common.tileentity.traits.BlockActivationListener;
import li.cil.oc.common.tileentity.traits.LocationTileEntityProxy;
import li.cil.oc.common.tileentity.traits.OpenSides;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public final class TileEntityNetSplitter extends AbstractTileEntitySingleNodeContainer implements BlockActivationListener, LocationTileEntityProxy, OpenSides.OpenSidesHost, RedstoneAwareImpl.RedstoneAwareHost {
    // ----------------------------------------------------------------------- //
    // Persisted data.

    private final NodeContainer nodeContainer = new NodeContainerNetSplitter(this);
    private final RedstoneAwareImpl redstone = new RedstoneAwareImpl(this);
    private final OpenSides sides = new OpenSides(this);

    // ----------------------------------------------------------------------- //
    // Computed data.

    // NBT tag names.
    private static final String TAG_REDSTONE = "redstone";
    private static final String TAG_SIDES = "sides";

    // ----------------------------------------------------------------------- //
    // TileEntity

    @Override
    public void onLoad() {
        super.onLoad();
        redstone.scheduleInputUpdate();
    }

    @Override
    public boolean hasCapability(final Capability<?> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityEnvironment.ENVIRONMENT_CAPABILITY && isSideClosed(facing)) {
            return false;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == CapabilityEnvironment.ENVIRONMENT_CAPABILITY && isSideClosed(facing)) {
            return null;
        }
        return super.getCapability(capability, facing);
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntity

    @Override
    protected void readFromNBTCommon(final NBTTagCompound nbt) {
        super.readFromNBTCommon(nbt);
        sides.deserializeNBT((NBTTagByte) nbt.getTag(TAG_SIDES));
        redstone.deserializeNBT((NBTTagCompound) nbt.getTag(TAG_REDSTONE));
    }

    @Override
    protected void writeToNBTCommon(final NBTTagCompound nbt) {
        super.writeToNBTCommon(nbt);
        nbt.setTag(TAG_SIDES, sides.serializeNBT());
        nbt.setTag(TAG_REDSTONE, redstone.serializeNBT());
    }

    // ----------------------------------------------------------------------- //
    // AbstractTileEntitySingleNodeContainer

    @Override
    protected NodeContainer getNodeContainer() {
        return nodeContainer;
    }

    // ----------------------------------------------------------------------- //
    // BlockActivationListener

    @Override
    public boolean onActivated(final EntityPlayer player, final EnumHand hand, final EnumFacing side, final float hitX, final float hitY, final float hitZ) {
        return sides.onActivated(player, hand, getPos(), side);
    }

    // ----------------------------------------------------------------------- //
    // OpenSidesHost

    @Override
    public void onSideOpenChanged(final EnumFacing side) {
        handleStateChanged(!isSideClosed(side));
    }

    // ----------------------------------------------------------------------- //
    // RedstoneAwareHost

    @Override
    public void onRedstoneInputChanged(final EnumFacing side, final int oldValue, final int newValue) {
        handleStateChanged(redstone.getMaxInput() > 0);
    }

    // ----------------------------------------------------------------------- //
    // TileEntityAccess

    @Override
    public TileEntity getTileEntity() {
        return this;
    }

    // ----------------------------------------------------------------------- //

    private void handleStateChanged(final boolean open) {
        final World world = getWorld();
        final SoundEvent sound = open ? SoundEvents.BLOCK_PISTON_EXTEND : SoundEvents.BLOCK_PISTON_CONTRACT;
        final Vec3d position = getHostPosition();
        world.playSound(null, position.xCoord, position.yCoord, position.zCoord, sound, SoundCategory.BLOCKS, 0.5f, world.rand.nextFloat() * 0.25f + 0.7f);
        world.notifyNeighborsOfStateChange(getPos(), getBlockType(), false);

//      ServerPacketSender.sendNetSplitterState(this)
//        getWorld.notifyBlockUpdate(getPos, getWorld.getBlockState(getPos), getWorld.getBlockState(getPos), 3)
    }

    private boolean isSideClosed(@Nullable final EnumFacing facing) {
        final boolean isInverted = redstone.getMaxInput() > 0;
        return sides.isSideOpen(facing) == isInverted;
    }

    // ----------------------------------------------------------------------- //

    private static final class NodeContainerNetSplitter extends AbstractTileEntityNodeContainer {
        NodeContainerNetSplitter(final TileEntity host) {
            super(host);
        }

        @Override
        protected Node createNode() {
            return Network.newNode(this, Visibility.NONE).create();
        }
    }
}