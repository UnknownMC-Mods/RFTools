package mcjty.rftools.blocks.screens;

import mcjty.lib.McJtyLib;
import mcjty.lib.blocks.BaseBlock;
import mcjty.lib.blocks.GenericBlock;
import mcjty.lib.container.EmptyContainer;
import mcjty.rftools.RFTools;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Random;

public class ScreenHitBlock extends GenericBlock<ScreenHitTileEntity, EmptyContainer> {

    public ScreenHitBlock() {
        super(RFTools.instance, Material.GLASS, ScreenHitTileEntity.class, EmptyContainer::new, null, "screen_hitblock", false);
        setBlockUnbreakable();
        setResistance(6000000.0F);
//        setTranslationKey("rftools.screen_hitblock");
//        setRegistryName("screen_hitblock");
//        GameRegistry.register(this);
//        GameRegistry.registerTileEntity(ScreenHitTileEntity.class, "screen_hitblock");
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        BlockPos screenPos = getScreenBlockPos(worldIn, pos);
        if(screenPos == null) return ItemStack.EMPTY;
        IBlockState screenState = worldIn.getBlockState(screenPos);
        return screenState.getBlock().getItem(worldIn, screenPos, screenState);
    }

    @Override
    public int getGuiID() {
        return -1;
    }

    @Override
    @Optional.Method(modid = "theoneprobe")
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        super.addProbeInfo(mode, probeInfo, player, world, blockState, data);
        BlockPos pos = data.getPos();
        ScreenHitTileEntity screenHitTileEntity = (ScreenHitTileEntity) world.getTileEntity(pos);
        int dx = screenHitTileEntity.getDx();
        int dy = screenHitTileEntity.getDy();
        int dz = screenHitTileEntity.getDz();
        Block block = world.getBlockState(pos.add(dx, dy, dz)).getBlock();
        if (block instanceof ScreenBlock) {
            ((ScreenBlock) block).addProbeInfoScreen(mode, probeInfo, player, world, pos.add(dx, dy, dz));
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    @Optional.Method(modid = "waila")
    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        super.getWailaBody(itemStack, currenttip, accessor, config);
        BlockPos pos = accessor.getPosition();
        World world = accessor.getWorld();
        ScreenHitTileEntity screenHitTileEntity = (ScreenHitTileEntity) world.getTileEntity(pos);
        int dx = screenHitTileEntity.getDx();
        int dy = screenHitTileEntity.getDy();
        int dz = screenHitTileEntity.getDz();
        BlockPos rpos = pos.add(dx, dy, dz);
        IBlockState state = world.getBlockState(rpos);
        Block block = state.getBlock();
        if (block instanceof ScreenBlock) {
            TileEntity te = world.getTileEntity(rpos);
            if (te instanceof ScreenTileEntity) {
                RayTraceResult mouseOver = accessor.getMOP();
                ScreenTileEntity screenTileEntity = (ScreenTileEntity) te;
                ScreenTileEntity.ModuleRaytraceResult hit = screenTileEntity.getHitModule(mouseOver.hitVec.x - pos.getX() - dx, mouseOver.hitVec.y - pos.getY() - dy, mouseOver.hitVec.z - pos.getZ() - dz, mouseOver.sideHit, state.getValue(ScreenBlock.HORIZONTAL_FACING));
                ((ScreenBlock) block).getWailaBodyScreen(currenttip, accessor.getPlayer(), screenTileEntity, hit);
            }
        }
        return currenttip;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new ScreenHitTileEntity();
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new ScreenHitTileEntity();
    }

    @Override
    public void initModel() {
        McJtyLib.proxy.initTESRItemStack(Item.getItemFromBlock(this), 0, ScreenTileEntity.class);
        super.initModel();
    }


    @Override
    public void onBlockClicked(World world, BlockPos pos, EntityPlayer playerIn) {
        if (world.isRemote) {
            ScreenHitTileEntity screenHitTileEntity = (ScreenHitTileEntity) world.getTileEntity(pos);
            int dx = screenHitTileEntity.getDx();
            int dy = screenHitTileEntity.getDy();
            int dz = screenHitTileEntity.getDz();
            IBlockState state = world.getBlockState(pos.add(dx, dy, dz));
            Block block = state.getBlock();
            if (block != ScreenSetup.screenBlock && block != ScreenSetup.creativeScreenBlock) {
                return;
            }

            RayTraceResult mouseOver = Minecraft.getMinecraft().objectMouseOver;
            ScreenTileEntity screenTileEntity = (ScreenTileEntity) world.getTileEntity(pos.add(dx, dy, dz));
            screenTileEntity.hitScreenClient(mouseOver.hitVec.x - pos.getX() - dx, mouseOver.hitVec.y - pos.getY() - dy, mouseOver.hitVec.z - pos.getZ() - dz, mouseOver.sideHit, state.getValue(ScreenBlock.HORIZONTAL_FACING));
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        return activate(world, pos, state, player, hand, side, hitX, hitY, hitZ);
    }

    public boolean activate(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        pos = getScreenBlockPos(world, pos);
        if (pos == null) {
            return false;
        }
        Block block = world.getBlockState(pos).getBlock();
        return ((ScreenBlock) block).activate(world, pos, state, player, hand, side, hitX, hitY, hitZ);
    }

    @Override
    public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis) {
        // Doesn't make sense to rotate a potentially 3x3 screen,
        // and is incompatible with our special wrench actions.
        return false;
    }

    public BlockPos getScreenBlockPos(World world, BlockPos pos) {
        ScreenHitTileEntity screenHitTileEntity = (ScreenHitTileEntity) world.getTileEntity(pos);
        int dx = screenHitTileEntity.getDx();
        int dy = screenHitTileEntity.getDy();
        int dz = screenHitTileEntity.getDz();
        pos = pos.add(dx, dy, dz);
        Block block = world.getBlockState(pos).getBlock();
        if (block != ScreenSetup.screenBlock && block != ScreenSetup.creativeScreenBlock) {
            return null;
        }
        return pos;
    }

    public static final AxisAlignedBB BLOCK_AABB = new AxisAlignedBB(0.5F - 0.5F, 0.0F, 0.5F - 0.5F, 0.5F + 0.5F, 1.0F, 0.5F + 0.5F);
    public static final AxisAlignedBB NORTH_AABB = new AxisAlignedBB(0.0F, 0.0F, 1.0F - 0.125F, 1.0F, 1.0F, 1.0F);
    public static final AxisAlignedBB SOUTH_AABB = new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.125F);
    public static final AxisAlignedBB WEST_AABB = new AxisAlignedBB(1.0F - 0.125F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    public static final AxisAlignedBB EAST_AABB = new AxisAlignedBB(0.0F, 0.0F, 0.0F, 0.125F, 1.0F, 1.0F);
    public static final AxisAlignedBB UP_AABB = new AxisAlignedBB(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
    public static final AxisAlignedBB DOWN_AABB = new AxisAlignedBB(0.0F, 1.0F - 0.125F, 0.0F, 1.0F, 1.0F, 1.0F);

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        EnumFacing facing = state.getValue(BaseBlock.FACING);
        if (facing == EnumFacing.NORTH) {
            return NORTH_AABB;
        } else if (facing == EnumFacing.SOUTH) {
            return SOUTH_AABB;
        } else if (facing == EnumFacing.WEST) {
            return WEST_AABB;
        } else if (facing == EnumFacing.EAST) {
            return EAST_AABB;
        } else if (facing == EnumFacing.UP) {
            return UP_AABB;
        } else if (facing == EnumFacing.DOWN) {
            return DOWN_AABB;
        } else {
            return BLOCK_AABB;
        }
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isBlockNormalCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullBlock(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.BLOCK;
    }
}
