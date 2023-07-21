package mcjty.rftools.items.builder;

import mcjty.lib.crafting.INBTPreservingIngredient;
import mcjty.lib.varia.*;
import mcjty.rftools.RFTools;
import mcjty.rftools.blocks.builder.BuilderConfiguration;
import mcjty.rftools.blocks.builder.BuilderTileEntity;
import mcjty.rftools.setup.GuiProxy;
import mcjty.rftools.items.GenericRFToolsItem;
import mcjty.rftools.shapes.*;
import mcjty.rftools.varia.RLE;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;

import java.io.*;
import java.util.*;

public class ShapeCardItem extends GenericRFToolsItem implements INBTPreservingIngredient {

    public static final int MAXIMUM_COUNT = 50000000;

    public static final int MODE_NONE = 0;
    public static final int MODE_CORNER1 = 1;
    public static final int MODE_CORNER2 = 2;

    public ShapeCardItem() {
        super("shape_card");
        setMaxStackSize(1);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void initModel() {
        for(ShapeCardType type : ShapeCardType.values()) {
            ModelResourceLocation modelResourceLocation = type.getModelResourceLocation();
            if(modelResourceLocation != null) {
                ModelLoader.setCustomModelResourceLocation(this, type.getDamage(), modelResourceLocation);
            }
        }
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 1;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote) {
            int mode = getMode(stack);
            if (mode == MODE_NONE) {
                if (player.isSneaking()) {
                    if (world.getTileEntity(pos) instanceof BuilderTileEntity) {
                        setCurrentBlock(stack, new GlobalCoordinate(pos, world.provider.getDimension()));
                        Logging.message(player, TextFormatting.GREEN + "Now select the first corner");
                        setMode(stack, MODE_CORNER1);
                        setCorner1(stack, null);
                    } else {
                        Logging.message(player, TextFormatting.RED + "You can only do this on a builder!");
                    }
                } else {
                    return EnumActionResult.SUCCESS;
                }
            } else if (mode == MODE_CORNER1) {
                GlobalCoordinate currentBlock = getCurrentBlock(stack);
                if (currentBlock.getDimension() != world.provider.getDimension()) {
                    Logging.message(player, TextFormatting.RED + "The Builder is in another dimension!");
                } else if (currentBlock.getCoordinate().equals(pos)) {
                    Logging.message(player, TextFormatting.RED + "Cleared area selection mode!");
                    setMode(stack, MODE_NONE);
                } else {
                    Logging.message(player, TextFormatting.GREEN + "Now select the second corner");
                    setMode(stack, MODE_CORNER2);
                    setCorner1(stack, pos);
                }
            } else {
                GlobalCoordinate currentBlock = getCurrentBlock(stack);
                if (currentBlock.getDimension() != world.provider.getDimension()) {
                    Logging.message(player, TextFormatting.RED + "The Builder is in another dimension!");
                } else if (currentBlock.getCoordinate().equals(pos)) {
                    Logging.message(player, TextFormatting.RED + "Cleared area selection mode!");
                    setMode(stack, MODE_NONE);
                } else {
                    NBTTagCompound tag = getCompound(stack);
                    BlockPos c1 = getCorner1(stack);
                    if (c1 == null) {
                        Logging.message(player, TextFormatting.RED + "Cleared area selection mode!");
                        setMode(stack, MODE_NONE);
                    } else {
                        Logging.message(player, TextFormatting.GREEN + "New settings copied to the shape card!");
                        BlockPos center = new BlockPos((int) Math.ceil((c1.getX() + pos.getX()) / 2.0f), (int) Math.ceil((c1.getY() + pos.getY()) / 2.0f), (int) Math.ceil((c1.getZ() + pos.getZ()) / 2.0f));
                        setDimension(stack, Math.abs(c1.getX() - pos.getX()) + 1, Math.abs(c1.getY() - pos.getY()) + 1, Math.abs(c1.getZ() - pos.getZ()) + 1);
                        setOffset(stack, center.getX() - currentBlock.getCoordinate().getX(), center.getY() - currentBlock.getCoordinate().getY(), center.getZ() - currentBlock.getCoordinate().getZ());

                        setMode(stack, MODE_NONE);
                        setCorner1(stack, null);
                        setShape(stack, Shape.SHAPE_BOX, true);
                    }
                }
            }
        }
        return EnumActionResult.SUCCESS;
    }

    public static void setData(NBTTagCompound tagCompound, int scanID) {
        tagCompound.setInteger("scanid", scanID);
    }

    public static void setModifier(NBTTagCompound tag, ShapeModifier modifier) {
        tag.setString("mod_op", modifier.getOperation().getCode());
        tag.setBoolean("mod_flipy", modifier.isFlipY());
        tag.setString("mod_rot", modifier.getRotation().getCode());
    }

    public static void setGhostMaterial(NBTTagCompound tag, ItemStack materialGhost) {
        if (materialGhost.isEmpty()) {
            tag.removeTag("ghost_block");
            tag.removeTag("ghost_meta");
        } else {
            Block block = Block.getBlockFromItem(materialGhost.getItem());
            if (block == null) {
                tag.removeTag("ghost_block");
                tag.removeTag("ghost_meta");
            } else {
                tag.setString("ghost_block", block.getRegistryName().toString());
                tag.setInteger("ghost_meta", materialGhost.getMetadata());
            }
        }
    }

    public static void setChildren(ItemStack itemStack, NBTTagList list) {
        NBTTagCompound tagCompound = getCompound(itemStack);
        tagCompound.setTag("children", list);
    }

    public static void setDimension(ItemStack itemStack, int x, int y, int z) {
        NBTTagCompound tagCompound = getCompound(itemStack);
        if (tagCompound.getInteger("dimX") == x && tagCompound.getInteger("dimY") == y && tagCompound.getInteger("dimZ") == z) {
            return;
        }
        tagCompound.setInteger("dimX", x);
        tagCompound.setInteger("dimY", y);
        tagCompound.setInteger("dimZ", z);
    }


    public static void setOffset(ItemStack itemStack, int x, int y, int z) {
        NBTTagCompound tagCompound = getCompound(itemStack);
        if (tagCompound.getInteger("offsetX") == x && tagCompound.getInteger("offsetY") == y && tagCompound.getInteger("offsetZ") == z) {
            return;
        }
        tagCompound.setInteger("offsetX", x);
        tagCompound.setInteger("offsetY", y);
        tagCompound.setInteger("offsetZ", z);
    }

    private static NBTTagCompound getCompound(ItemStack itemStack) {
        NBTTagCompound tagCompound = itemStack.getTagCompound();
        if (tagCompound == null) {
            tagCompound = new NBTTagCompound();
            itemStack.setTagCompound(tagCompound);
        }
        return tagCompound;
    }

    public static void setCorner1(ItemStack itemStack, BlockPos corner) {
        NBTTagCompound tagCompound = getCompound(itemStack);
        if (corner == null) {
            tagCompound.removeTag("corner1x");
            tagCompound.removeTag("corner1y");
            tagCompound.removeTag("corner1z");
        } else {
            tagCompound.setInteger("corner1x", corner.getX());
            tagCompound.setInteger("corner1y", corner.getY());
            tagCompound.setInteger("corner1z", corner.getZ());
        }
    }

    public static BlockPos getCorner1(ItemStack stack1) {
        NBTTagCompound tagCompound = stack1.getTagCompound();
        if (tagCompound == null) {
            return null;
        }
        if (!tagCompound.hasKey("corner1x")) {
            return null;
        }
        return new BlockPos(tagCompound.getInteger("corner1x"), tagCompound.getInteger("corner1y"), tagCompound.getInteger("corner1z"));
    }

    public static int getMode(ItemStack itemStack) {
        NBTTagCompound tagCompound = itemStack.getTagCompound();
        if (tagCompound != null) {
            return tagCompound.getInteger("mode");
        } else {
            return MODE_NONE;
        }
    }

    public static void setMode(ItemStack itemStack, int mode) {
        NBTTagCompound tagCompound = getCompound(itemStack);
        if (tagCompound.getInteger("mode") == mode) {
            return;
        }
        tagCompound.setInteger("mode", mode);
    }

    public static void setCurrentBlock(ItemStack itemStack, GlobalCoordinate c) {
        NBTTagCompound tagCompound = getCompound(itemStack);

        if (c == null) {
            tagCompound.removeTag("selectedX");
            tagCompound.removeTag("selectedY");
            tagCompound.removeTag("selectedZ");
            tagCompound.removeTag("selectedDim");
        } else {
            tagCompound.setInteger("selectedX", c.getCoordinate().getX());
            tagCompound.setInteger("selectedY", c.getCoordinate().getY());
            tagCompound.setInteger("selectedZ", c.getCoordinate().getZ());
            tagCompound.setInteger("selectedDim", c.getDimension());
        }
    }

    public static GlobalCoordinate getCurrentBlock(ItemStack itemStack) {
        NBTTagCompound tagCompound = itemStack.getTagCompound();
        if (tagCompound != null && tagCompound.hasKey("selectedX")) {
            int x = tagCompound.getInteger("selectedX");
            int y = tagCompound.getInteger("selectedY");
            int z = tagCompound.getInteger("selectedZ");
            int dim = tagCompound.getInteger("selectedDim");
            return new GlobalCoordinate(new BlockPos(x, y, z), dim);
        }
        return null;
    }


    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack itemStack, World player, List<String> list, ITooltipFlag whatIsThis) {
        super.addInformation(itemStack, player, list, whatIsThis);

        ShapeCardType type = ShapeCardType.fromDamage(itemStack.getItemDamage());
        if (!BuilderConfiguration.shapeCardAllowed.get()) {
            list.add(TextFormatting.RED + "Disabled in config!");
        } else if (type != ShapeCardType.CARD_SHAPE) {
            if (!BuilderConfiguration.quarryAllowed.get()) {
                list.add(TextFormatting.RED + "Disabled in config!");
            } else if (type.isQuarry() && type.isClearing()) {
                if (!BuilderConfiguration.clearingQuarryAllowed.get()) {
                    list.add(TextFormatting.RED + "Disabled in config!");
                }
            }
        }

        Shape shape = getShape(itemStack);
        boolean issolid = isSolid(itemStack);
        list.add(TextFormatting.GREEN + "Shape " + shape.getDescription() + " (" + (issolid ? "Solid" : "Hollow") + ")");
        list.add(TextFormatting.GREEN + "Dimension " + BlockPosTools.toString(getDimension(itemStack)));
        list.add(TextFormatting.GREEN + "Offset " + BlockPosTools.toString(getOffset(itemStack)));

        if (shape.isComposition()) {
            NBTTagCompound card = itemStack.getTagCompound();
            NBTTagList children = card.getTagList("children", Constants.NBT.TAG_COMPOUND);
            list.add(TextFormatting.DARK_GREEN + "Formulas: " + children.tagCount());
        }

        if (shape.isScan()) {
            NBTTagCompound card = itemStack.getTagCompound();
            int scanid = card.getInteger("scanid");
            list.add(TextFormatting.DARK_GREEN + "Scan id: " + scanid);
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
            list.add(TextFormatting.YELLOW + "Sneak right click on builder to start mark mode");
            list.add(TextFormatting.YELLOW + "Then right click to mark two corners of wanted area");
            type.addInformation(list);
        } else {
            list.add(TextFormatting.WHITE + GuiProxy.SHIFT_MESSAGE);
        }
    }

    /**
     * Return true if the card is a normal card (not a quarry or void card)
     * @param stack
     * @return
     */
    public static boolean isNormalShapeCard(ItemStack stack) {
        int damage = stack.getItemDamage();
        return damage == ShapeCardType.CARD_SHAPE.getDamage() || damage == ShapeCardType.CARD_PUMP_LIQUID.getDamage();
    }

    private static void addBlocks(Set<Block> blocks, Block block, boolean oredict) {
        blocks.add(block);
        if (oredict) {
            int[] iDs = OreDictionary.getOreIDs(new ItemStack(block));
            for (int id : iDs) {
                String oreName = OreDictionary.getOreName(id);
                List<ItemStack> ores = ItemStackTools.getOres(oreName);
                for (ItemStack ore : ores) {
                    if (ore.getItem() instanceof ItemBlock) {
                        blocks.add(((ItemBlock)ore.getItem()).getBlock());
                    }
                }
            }
        }
    }

    public static Set<Block> getVoidedBlocks(ItemStack stack) {
        Set<Block> blocks = new HashSet<>();
        boolean oredict = isOreDictionary(stack);
        if (isVoiding(stack, "stone")) {
            addBlocks(blocks, Blocks.STONE, oredict);
        }
        if (isVoiding(stack, "cobble")) {
            addBlocks(blocks, Blocks.COBBLESTONE, oredict);
        }
        if (isVoiding(stack, "dirt")) {
            addBlocks(blocks, Blocks.DIRT, oredict);
            addBlocks(blocks, Blocks.GRASS, oredict);
        }
        if (isVoiding(stack, "sand")) {
            addBlocks(blocks, Blocks.SAND, oredict);
        }
        if (isVoiding(stack, "gravel")) {
            addBlocks(blocks, Blocks.GRAVEL, oredict);
        }
        if (isVoiding(stack, "netherrack")) {
            addBlocks(blocks, Blocks.NETHERRACK, oredict);
        }
        if (isVoiding(stack, "endstone")) {
            addBlocks(blocks, Blocks.END_STONE, oredict);
        }
        return blocks;
    }

    public static boolean isOreDictionary(ItemStack stack) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null) {
            return false;
        }
        return tagCompound.getBoolean("oredict");
    }

    public static boolean isVoiding(ItemStack stack, String material) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null) {
            return false;
        }
        return tagCompound.getBoolean("void" + material);
    }

    public static Shape getShape(ItemStack stack) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        return getShape(tagCompound);
    }

    public static Shape getShape(NBTTagCompound tagCompound) {
        if (tagCompound == null) {
            return Shape.SHAPE_BOX;
        }
        if (!tagCompound.hasKey("shape") && !tagCompound.hasKey("shapenew")) {
            return Shape.SHAPE_BOX;
        }
        Shape shape;
        if (tagCompound.hasKey("shapenew")) {
            String sn = tagCompound.getString("shapenew");
            shape = Shape.getShape(sn);
        } else {
            int shapedeprecated = tagCompound.getInteger("shape");
            ShapeDeprecated sd = ShapeDeprecated.getShape(shapedeprecated);
            shape = sd.getNewshape();
        }

        if (shape == null) {
            return Shape.SHAPE_BOX;
        }
        return shape;
    }

    public static boolean isSolid(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        NBTTagCompound tagCompound = stack.getTagCompound();
        return isSolid(tagCompound);
    }

    public static boolean isSolid(NBTTagCompound tagCompound) {
        if (tagCompound == null) {
            return true;
        }
        if (!tagCompound.hasKey("shape") && !tagCompound.hasKey("shapenew")) {
            return true;
        }
        if (tagCompound.hasKey("shapenew")) {
            return tagCompound.getBoolean("solid");
        } else {
            int shapedeprecated = tagCompound.getInteger("shape");
            ShapeDeprecated sd = ShapeDeprecated.getShape(shapedeprecated);
            return sd.isSolid();
        }
    }

    public static IFormula createCorrectFormula(NBTTagCompound tagCompound) {
        Shape shape = getShape(tagCompound);
        boolean solid = isSolid(tagCompound);
        IFormula formula = shape.getFormulaFactory().get();
        return formula.correctFormula(solid);
    }

    public static int getScanId(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        NBTTagCompound tagCompound = getCompound(stack);
        Shape shape = getShape(tagCompound);
        if (shape != Shape.SHAPE_SCAN) {
            return 0;
        }
        return tagCompound.getInteger("scanid");
    }

    // Also find scanId's from children
    public static int getScanIdRecursive(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return getScanIdRecursive(getCompound(stack));
    }

    private static int getScanIdRecursive(NBTTagCompound tagCompound) {
        Shape shape = getShape(tagCompound);
        if (tagCompound.hasKey("scanid") && shape == Shape.SHAPE_SCAN) {
            return tagCompound.getInteger("scanid");
        }
        if (shape == Shape.SHAPE_COMPOSITION) {
            // See if there is a scan in the composition that has a scan id
            NBTTagList children = tagCompound.getTagList("children", Constants.NBT.TAG_COMPOUND);
            for (int i = 0 ; i < children.tagCount() ; i++) {
                NBTTagCompound childTag = children.getCompoundTagAt(i);
                int id = getScanIdRecursive(childTag);
                if (id != 0) {
                    return id;
                }
            }
        }
        return 0;
    }

    public static int getFormulaCheckClient(ItemStack stack) {
        Check32 crc = new Check32();
        getFormulaCheckClient(stack, crc);
        return crc.get();
    }

    public static void getFormulaCheckClient(ItemStack stack, Check32 crc) {
        Shape shape = getShape(stack);
        IFormula formula = shape.getFormulaFactory().get();
        formula.getCheckSumClient(stack.getTagCompound(), crc);
    }

    public static void getLocalChecksum(NBTTagCompound tagCompound, Check32 crc) {
        if (tagCompound == null) {
            return;
        }
        crc.add(getShape(tagCompound).ordinal());
        BlockPos dim = getDimension(tagCompound);
        crc.add(dim.getX());
        crc.add(dim.getY());
        crc.add(dim.getZ());
        crc.add(isSolid(tagCompound) ? 1 : 0);
    }



    public static void setShape(ItemStack stack, Shape shape, boolean solid) {
        NBTTagCompound tagCompound = getCompound(stack);
        if (isSolid(tagCompound) == solid && getShape(tagCompound).equals(shape)) {
            // Nothing happens
            return;
        }
        tagCompound.setString("shapenew", shape.getDescription());
        tagCompound.setBoolean("solid", solid);
    }

    public static BlockPos getDimension(ItemStack stack) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        return getDimension(tagCompound);
    }

    public static BlockPos getDimension(NBTTagCompound tagCompound) {
        if (tagCompound == null) {
            return new BlockPos(5, 5, 5);
        }
        if (!tagCompound.hasKey("dimX")) {
            return new BlockPos(5, 5, 5);
        }
        int dimX = tagCompound.getInteger("dimX");
        int dimY = tagCompound.getInteger("dimY");
        int dimZ = tagCompound.getInteger("dimZ");
        return new BlockPos(dimX, clampDimension(dimY, 256), dimZ);
    }

    public static BlockPos getClampedDimension(ItemStack stack, int maximum) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        return getClampedDimension(tagCompound, maximum);
    }

    public static BlockPos getClampedDimension(NBTTagCompound tagCompound, int maximum) {
        if (tagCompound == null) {
            return new BlockPos(5, 5, 5);
        }
        int dimX = tagCompound.getInteger("dimX");
        int dimY = tagCompound.getInteger("dimY");
        int dimZ = tagCompound.getInteger("dimZ");
        return new BlockPos(clampDimension(dimX, maximum), clampDimension(dimY, maximum), clampDimension(dimZ, maximum));
    }

    private static int clampDimension(int o, int maximum) {
        if (o > maximum) {
            o = maximum;
        } else if (o < 0) {
            o = 0;
        }
        return o;
    }

    public static BlockPos getOffset(ItemStack stack) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        if (tagCompound == null) {
            return new BlockPos(0, 0, 0);
        }
        int offsetX = tagCompound.getInteger("offsetX");
        int offsetY = tagCompound.getInteger("offsetY");
        int offsetZ = tagCompound.getInteger("offsetZ");
        return new BlockPos(offsetX, offsetY, offsetZ);
    }

    public static BlockPos getClampedOffset(ItemStack stack, int maximum) {
        NBTTagCompound tagCompound = stack.getTagCompound();
        return getClampedOffset(tagCompound, maximum);
    }

    public static BlockPos getClampedOffset(NBTTagCompound tagCompound, int maximum) {
        if (tagCompound == null) {
            return new BlockPos(0, 0, 0);
        }
        int offsetX = tagCompound.getInteger("offsetX");
        int offsetY = tagCompound.getInteger("offsetY");
        int offsetZ = tagCompound.getInteger("offsetZ");
        return new BlockPos(clampOffset(offsetX, maximum), clampOffset(offsetY, maximum), clampOffset(offsetZ, maximum));
    }

    private static int clampOffset(int o, int maximum) {
        if (o < -maximum) {
            o = -maximum;
        } else if (o > maximum) {
            o = maximum;
        }
        return o;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            player.openGui(RFTools.instance, GuiProxy.GUI_SHAPECARD, player.getEntityWorld(), (int) player.posX, (int) player.posY, (int) player.posZ);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    public static BlockPos getMinCorner(BlockPos thisCoord, BlockPos dimension, BlockPos offset) {
        int xCoord = thisCoord.getX();
        int yCoord = thisCoord.getY();
        int zCoord = thisCoord.getZ();
        int dx = dimension.getX();
        int dy = dimension.getY();
        int dz = dimension.getZ();
        return new BlockPos(xCoord - dx/2 + offset.getX(), yCoord - dy/2 + offset.getY(), zCoord - dz/2 + offset.getZ());
    }

    public static BlockPos getMaxCorner(BlockPos thisCoord, BlockPos dimension, BlockPos offset) {
        int dx = dimension.getX();
        int dy = dimension.getY();
        int dz = dimension.getZ();
        BlockPos minCorner = getMinCorner(thisCoord, dimension, offset);
        return new BlockPos(minCorner.getX() + dx, minCorner.getY() + dy, minCorner.getZ() + dz);
    }

    public static boolean xInChunk(int x, ChunkPos chunk) {
        if (chunk == null) {
            return true;
        } else {
            return chunk.x == (x>>4);
        }
    }

    public static boolean zInChunk(int z, ChunkPos chunk) {
        if (chunk == null) {
            return true;
        } else {
            return chunk.z == (z>>4);
        }
    }

    private static void placeBlockIfPossible(World worldObj, Map<BlockPos, IBlockState> blocks, int maxSize, int x, int y, int z, IBlockState state, boolean forquarry) {
        BlockPos c = new BlockPos(x, y, z);
        if (worldObj == null) {
            blocks.put(c, state);
            return;
        }
        if (forquarry) {
            if (worldObj.isAirBlock(c)) {
                return;
            }
            blocks.put(c, state);
        } else {
            if (BuilderTileEntity.isEmptyOrReplacable(worldObj, c) && blocks.size() < maxSize) {
                blocks.put(c, state);
            }
        }
    }

    public static int getRenderPositions(ItemStack stack, boolean solid, RLE positions, StatePalette statePalette, IFormula formula, int oy) {
        BlockPos dimension = ShapeCardItem.getDimension(stack);
        BlockPos clamped = new BlockPos(Math.min(dimension.getX(), 512), Math.min(dimension.getY(), 256), Math.min(dimension.getZ(), 512));

        int dx = clamped.getX();
        int dy = clamped.getY();
        int dz = clamped.getZ();

        int cnt = 0;
        int y = oy - dy / 2;
        for (int ox = 0; ox < dx; ox++) {
            int x = ox - dx / 2;
            for (int oz = 0; oz < dz; oz++) {
                int z = oz - dz / 2;
                int v = 255;
                if (formula.isInside(x, y, z)) {
                    cnt++;
                    IBlockState lastState = formula.getLastState();
                    if (solid) {
                        if (ox == 0 || ox == dx - 1 || oy == 0 || oy == dy - 1 || oz == 0 || oz == dz - 1) {
                            v = statePalette.alloc(lastState, -1) + 1;
                        } else if (formula.isVisible(x, y, z)) {
                            v = statePalette.alloc(lastState, -1) + 1;
                        }
                    } else {
                        v = statePalette.alloc(lastState, -1) + 1;
                    }
                }
                positions.add(v);
            }
        }
        return cnt;
    }


    // Used for saving
    public static int getDataPositions(ItemStack stack, Shape shape, boolean solid, RLE positions, StatePalette statePalette) {
        BlockPos dimension = ShapeCardItem.getDimension(stack);
        BlockPos clamped = new BlockPos(Math.min(dimension.getX(), 512), Math.min(dimension.getY(), 256), Math.min(dimension.getZ(), 512));

        IFormula formula = shape.getFormulaFactory().get();
        int dx = clamped.getX();
        int dy = clamped.getY();
        int dz = clamped.getZ();

        formula = formula.correctFormula(solid);
        formula.setup(new BlockPos(0, 0, 0), clamped, new BlockPos(0, 0, 0), stack != null ? stack.getTagCompound() : null);

        // For saving shape cards we need to do X/Z/Y (scanner order) instead of the usual Y/X/Z (render order)
        int cnt = 0;
        for (int ox = 0; ox < dx; ox++) {
            int x = ox - dx/2;
            for (int oz = 0; oz < dz; oz++) {
                int z = oz - dz/2;
                for (int oy = 0; oy < dy; oy++) {
                    int y = oy - dy/2;
                    int v = 255;
                    if (formula.isInside(x, y, z)) {
                        cnt++;
                        IBlockState lastState = formula.getLastState();
                        if (lastState == null) {
                            lastState = Blocks.STONE.getDefaultState();
                        }
                        v = statePalette.alloc(lastState, 0) + 1;
                    }
                    positions.add(v);
                }
            }
        }
        return cnt;
    }



    public static void composeFormula(ItemStack shapeCard, IFormula formula, World worldObj, BlockPos thisCoord, BlockPos dimension, BlockPos offset, Map<BlockPos, IBlockState> blocks, int maxSize, boolean solid, boolean forquarry, ChunkPos chunk) {
        int xCoord = thisCoord.getX();
        int yCoord = thisCoord.getY();
        int zCoord = thisCoord.getZ();
        int dx = dimension.getX();
        int dy = dimension.getY();
        int dz = dimension.getZ();
        BlockPos tl = new BlockPos(xCoord - dx/2 + offset.getX(), yCoord - dy/2 + offset.getY(), zCoord - dz/2 + offset.getZ());

        formula = formula.correctFormula(solid);
        formula.setup(thisCoord, dimension, offset, shapeCard != null ? shapeCard.getTagCompound() : null);

        for (int ox = 0 ; ox < dx ; ox++) {
            int x = tl.getX() + ox;
            if (xInChunk(x, chunk)) {
                for (int oz = 0 ; oz < dz ; oz++) {
                    int z = tl.getZ() + oz;
                    if (zInChunk(z, chunk)) {
                        for (int oy = 0; oy < dy; oy++) {
                            int y = tl.getY() + oy;
//                            if (y >= yCoord-dy/2 && y < yCoord+dy/2) {    @todo!!!
                                if (formula.isInside(x, y, z)) {
                                    placeBlockIfPossible(worldObj, blocks, maxSize, x, y, z, formula.getLastState(), forquarry);
                                }
//                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getTranslationKey(ItemStack itemStack) {
        if (itemStack.getItemDamage() == 0) {
            return super.getTranslationKey(itemStack);
        } else {
            return super.getTranslationKey(itemStack) + itemStack.getItemDamage();
        }
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (this.isInCreativeTab(tab)) {
            for(ShapeCardType type : ShapeCardType.values()) {
                int damage = type.getDamage();
                if(damage >= 0) {
                    items.add(new ItemStack(this, 1, damage));
                }
            }
        }
    }

    private static boolean validFile(EntityPlayer player, String filename) {
        if (filename.contains("\\") || filename.contains("/") || filename.contains(":")) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Invalid filename '" + filename + "'! Cannot be a path!"), false);
            return false;
        }
        return true;
    }


    public static void save(EntityPlayer player, ItemStack card, String filename) {
        if (!validFile(player, filename)) {
            return;
        }

        Shape shape = ShapeCardItem.getShape(card);
        boolean solid = ShapeCardItem.isSolid(card);
        BlockPos offset = ShapeCardItem.getOffset(card);
        BlockPos dimension = ShapeCardItem.getDimension(card);

        RLE positions = new RLE();
        StatePalette statePalette = new StatePalette();
        int cnt = getDataPositions(card, shape, solid, positions, statePalette);

        byte[] data = positions.getData();

        File dataDir = new File("rftoolsscans");
        dataDir.mkdirs();
        File file = new File(dataDir, filename);
        try(PrintWriter writer = new PrintWriter(new FileOutputStream(file))) {
            writer.println("SHAPE");
            writer.println("DIM:" + dimension.getX() + "," + dimension.getY() + "," + dimension.getZ());
            writer.println("OFF:" + offset.getX() + "," + offset.getY() + "," + offset.getZ());
            for (IBlockState state : statePalette.getPalette()) {
                String r = state.getBlock().getRegistryName().toString();
                writer.println(r + "@" + state.getBlock().getMetaFromState(state));
            }
            writer.println("DATA");

            byte[] encoded = Base64.getEncoder().encode(data);
            writer.write(new String(encoded));
        } catch (FileNotFoundException e) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Cannot write to file '" + filename + "'!"), false);
            return;
        }
        player.sendStatusMessage(new TextComponentString(TextFormatting.GREEN + "Saved shape to file '" + file.getPath() + "'"), false);
    }

    public static void load(EntityPlayer player, ItemStack card, String filename) {
        if (!validFile(player, filename)) {
            return;
        }

        Shape shape = ShapeCardItem.getShape(card);

        if (shape != Shape.SHAPE_SCAN) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "To load a file into this card you need a linked 'scan' type card!"), false);
            return;
        }

        NBTTagCompound compound = ShapeCardItem.getCompound(card);
        int scanId = compound.getInteger("scanid");
        if (scanId == 0) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "This card is not linked to scan data!"), false);
            return;
        }

        File dataDir = new File("rftoolsscans");
        dataDir.mkdirs();
        File file = new File(dataDir, filename);

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String s = reader.readLine();
            if (!"SHAPE".equals(s)) {
                player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "This does not appear to be a valid shapecard file!"), false);
                return;
            }
            s = reader.readLine();
            if (!s.startsWith("DIM:")) {
                player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "This does not appear to be a valid shapecard file!"), false);
                return;
            }
            BlockPos dim = parse(s.substring(4));
            s = reader.readLine();
            if (!s.startsWith("OFF:")) {
                player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "This does not appear to be a valid shapecard file!"), false);
                return;
            }
            BlockPos off = parse(s.substring(4));
            s = reader.readLine();
            StatePalette statePalette = new StatePalette();
            while (!"DATA".equals(s)) {
                String[] split = StringUtils.split(s, '@');
                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(split[0]));
                int meta = Integer.parseInt(split[1]);
                if (block == null) {
                    player.sendStatusMessage(new TextComponentString(TextFormatting.YELLOW + "Could not find block '" + split[0] + "'!"), false);
                    block = Blocks.STONE;
                    meta = 0;
                }
                statePalette.add(block.getStateFromMeta(meta));
                s = reader.readLine();
            }
            s = reader.readLine();
            byte[] decoded = Base64.getDecoder().decode(s.getBytes());

            setDataFromFile(scanId, card, dim, off, decoded, statePalette);
        } catch (FileNotFoundException e) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Cannot read from file '" + filename + "'!"), false);
            return;
        } catch (IOException e) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Cannot read from file '" + filename + "'!"), false);
            return;
        } catch (NullPointerException e) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "File '" + filename + "' is too short!"), false);
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "File '" + filename + "' contains invalid entries!"), false);
            return;
        }
        player.sendStatusMessage(new TextComponentString(TextFormatting.GREEN + "Loaded shape from file '" + file.getPath() + "'"), false);
    }

    private static void setDataFromFile(int scanId, ItemStack card, BlockPos dimension, BlockPos offset, byte[] data, StatePalette palette) {
        ScanDataManager scans = ScanDataManager.getScans();
        scans.getOrCreateScan(scanId).setData(data, palette.getPalette(), dimension, offset);
        scans.save(scanId);
        ShapeCardItem.setDimension(card, dimension.getX(), dimension.getY(), dimension.getZ());
        ShapeCardItem.setOffset(card, offset.getX(), offset.getY(), offset.getZ());
        ShapeCardItem.setShape(card, Shape.SHAPE_SCAN, true);
    }


    private static BlockPos parse(String s) {
        String[] split = StringUtils.split(s, ',');
        return new BlockPos(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
    }
}
