package com.lowdragmc.mbd2;

import com.lowdragmc.mbd2.client.ClientProxy;
import com.lowdragmc.mbd2.common.CommonProxy;
import com.lowdragmc.lowdraglib.Platform;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

@Mod(MBD2.MOD_ID)
public class MBD2 {
    public static final String MOD_ID = "mbd2";
    public static final String NAME = "Multiblocked2";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);
    public static final Random RND = new Random();

    public MBD2() {
        MBD2.init();
        DistExecutor.unsafeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
    }

    public static void init() {
        LOGGER.info("{} is initializing on platform: {}", NAME, Platform.platformName());
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}