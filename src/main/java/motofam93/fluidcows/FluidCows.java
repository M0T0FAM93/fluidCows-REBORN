package motofam93.fluidcows;

import com.mojang.logging.LogUtils;
import motofam93.fluidcows.client.ClientKeybinds;
import motofam93.fluidcows.client.ClientWarmup;
import motofam93.fluidcows.client.TexturedMaskCache;
import motofam93.fluidcows.command.FluidCowsCommands;
import motofam93.fluidcows.util.EnabledFluids;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(FluidCows.MOD_ID)
public class FluidCows {
    public static final String MOD_ID = "fluidcows";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FluidCows(IEventBus modBus) {
        ModRegistries.init(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(ModEvents::registerAttributes);
        modBus.addListener(ModSpawns::onCommonSetup);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(this::clientSetup);
            modBus.addListener(this::onRegisterReloadListeners);
            modBus.addListener(ClientKeybinds::registerKeyMappings);
        }

        NeoForge.EVENT_BUS.addListener(FluidCowsCommands::onRegisterCommands);
    }

    private void commonSetup(final FMLCommonSetupEvent evt) {
        evt.enqueueWork(() -> {
            FluidCowsMainConfig.loadOrCreate();
            FluidCowConfigGenerator.generateAll();
            EnabledFluids.reloadFromDisk();
            motofam93.fluidcows.util.BreedingManager.reload();
        });
    }

    private void clientSetup(final FMLClientSetupEvent evt) {
        FluidCowRenderers.register();
        ClientWarmup.warmupAll();
    }

    private void onRegisterReloadListeners(RegisterClientReloadListenersEvent evt) {
        evt.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }
            @Override
            protected void apply(Void data, ResourceManager resourceManager, ProfilerFiller profiler) {
                TexturedMaskCache.clear();
                ClientWarmup.warmupAll();
            }
        });
    }
}
