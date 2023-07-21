package mcjty.rftools.blocks.teleporter;

import mcjty.lib.McJtyRegister;
import mcjty.rftools.RFTools;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class DestinationAnalyzerBlock extends Block {

    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    public DestinationAnalyzerBlock() {
        super(Material.IRON);
        setTranslationKey("rftools.destination_analyzer");
        setRegistryName("destination_analyzer");
        setCreativeTab(RFTools.setup.getTab());
        setHardness(2.0f);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 0);
        setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        McJtyRegister.registerLater(this, RFTools.instance, ItemBlock::new);
    }

    @SideOnly(Side.CLIENT)
    public void initModel() {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, new ModelResourceLocation(getRegistryName(), "inventory"));
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer).withProperty(FACING, getFacingFromEntity(pos, placer));
    }

    public static EnumFacing getFacingFromEntity(BlockPos clickedBlock, EntityLivingBase entityIn) {
        if (MathHelper.abs((float) entityIn.posX - clickedBlock.getX()) < 2.0F && MathHelper.abs((float) entityIn.posZ - clickedBlock.getZ()) < 2.0F) {
            double d0 = entityIn.posY + entityIn.getEyeHeight();

            if (d0 - clickedBlock.getY() > 2.0D) {
                return EnumFacing.UP;
            }

            if (clickedBlock.getY() - d0 > 0.0D) {
                return EnumFacing.DOWN;
            }
        }

        return entityIn.getHorizontalFacing().getOpposite();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, getFacing(meta));
    }

    public static EnumFacing getFacing(int meta) {
        int i = meta & 7;
        return EnumFacing.byIndex(i);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }
}
