package mcjty.rftools.blocks.shield;

import mcjty.lib.McJtyRegister;
import mcjty.lib.blocks.DamageMetadataItemBlock;
import mcjty.rftools.RFTools;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collection;

public class ShieldTemplateBlock extends Block {

    public static boolean activateBlock(Block block, World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        return block.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
    }

    public static Collection<IProperty<?>> getPropertyKeys(IBlockState state) {
        return state.getPropertyKeys();
    }

    public static enum TemplateColor implements IStringSerializable {
        BLUE("blue"), RED("red"), GREEN("green"), YELLOW("yellow");

        private final String name;

        TemplateColor(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    @SideOnly(Side.CLIENT)
    public void initModel() {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, new ModelResourceLocation(getRegistryName() + "_blue", "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 1, new ModelResourceLocation(getRegistryName() + "_red", "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 2, new ModelResourceLocation(getRegistryName() + "_green", "inventory"));
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 3, new ModelResourceLocation(getRegistryName() + "_yellow", "inventory"));
    }

    public static final PropertyEnum<TemplateColor> COLOR = PropertyEnum.<TemplateColor>create("color", TemplateColor.class);

    public ShieldTemplateBlock() {
        super(Material.GLASS);
        setTranslationKey("rftools.shield_template_block");
        setRegistryName("shield_template_block");
        setCreativeTab(RFTools.setup.getTab());
        McJtyRegister.registerLater(this, RFTools.instance, DamageMetadataItemBlock::new);
        setDefaultState(this.blockState.getBaseState().withProperty(COLOR, TemplateColor.BLUE));
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(COLOR).ordinal();
    }

    @Override
    public void getSubBlocks(CreativeTabs itemIn, NonNullList<ItemStack> items) {
        for (TemplateColor enumdyecolor : TemplateColor.values()) {
            items.add(new ItemStack(this, 1, enumdyecolor.ordinal()));
        }
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(COLOR, TemplateColor.values()[meta & 3]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(COLOR).ordinal();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, COLOR);
    }
}
