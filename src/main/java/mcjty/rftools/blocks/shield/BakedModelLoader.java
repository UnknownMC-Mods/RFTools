package mcjty.rftools.blocks.shield;

import mcjty.rftools.RFTools;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;

public class BakedModelLoader implements ICustomModelLoader {

    public static final CamoModel MIMIC_MODEL = new CamoModel();

    @Override
    public boolean accepts(ResourceLocation modelLocation) {
        if (!modelLocation.getNamespace().equals(RFTools.MODID)) {
            return false;
        }
        if (modelLocation instanceof ModelResourceLocation && ((ModelResourceLocation)modelLocation).getVariant().equals("inventory")) {
            return false;
        }
        return CamoShieldBlock.CAMO.equals(modelLocation.getPath());
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation) {
        if (CamoShieldBlock.CAMO.equals(modelLocation.getPath())) {
            return MIMIC_MODEL;
        }
        return null;
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {

    }
}
