package motofam93.fluidcows.client;

import com.mojang.blaze3d.platform.NativeImage;
import motofam93.fluidcows.FluidCows;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class TexturedMaskCache {
    private TexturedMaskCache() {}

    private static final Map<ResourceLocation, ResourceLocation> OVERLAY_CACHE = new HashMap<>();

    private static final Path CONFIG_TILE_ROOT = Paths.get("config", "fluidcows", "fluid_textures");
    private static final Path CONFIG_MASK_ROOT = Paths.get("config", "fluidcows", "generated_masks");
    private static final Path CONFIG_MASK_OVERRIDE = Paths.get("config", "fluidcows", "cow_white_mask.png");

    private static final ResourceLocation ASSET_MASK_PREFERRED =
            ResourceLocation.fromNamespaceAndPath("fluidcows", "textures/entity/cow/cow_white_mask.png");
    private static final ResourceLocation ASSET_MASK_FALLBACK =
            ResourceLocation.fromNamespaceAndPath("fluidcows", "textures/entity/cow_mask_white.png");

    private static final boolean FLIP_SAMPLE_Y = true;
    private static final int MODEL_W = 64, MODEL_H = 32;

    public static void clear() {
        OVERLAY_CACHE.clear();
    }

    public static ResourceLocation get(Fluid fluid) {
        if (fluid == null) return ASSET_MASK_PREFERRED;

        ResourceLocation fkey = BuiltInRegistries.FLUID.getKey(fluid);
        Path genFile = CONFIG_MASK_ROOT.resolve(Paths.get(fkey.getNamespace(), fkey.getPath() + ".png"));
        ResourceLocation cached = OVERLAY_CACHE.get(fkey);

        if (cached != null && !cached.equals(ASSET_MASK_PREFERRED) && isGoodFile(genFile)) {
            return cached;
        }

        if (isGoodFile(genFile)) {
            ResourceLocation rl = generatedRLFor(fkey.getNamespace(), fkey.getPath());
            try (InputStream in = Files.newInputStream(genFile)) {
                NativeImage img = NativeImage.read(in);
                Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(img));
                OVERLAY_CACHE.put(fkey, rl);
                FluidCows.LOGGER.debug("Registered existing generated overlay: {} from {}", rl, genFile);
                return rl;
            } catch (Throwable t) {
                FluidCows.LOGGER.error("Failed registering existing generated overlay", t);
            }
        }

        ResourceLocation composed = composeOverlay(fkey, fluid);

        if (composed != null && !composed.equals(ASSET_MASK_PREFERRED)) {
            OVERLAY_CACHE.put(fkey, composed);
            return composed;
        }

        return ASSET_MASK_PREFERRED;
    }

    private static NativeImage loadMask(ResourceManager rm) {
        try {
            if (Files.exists(CONFIG_MASK_OVERRIDE)) {
                try (InputStream in = Files.newInputStream(CONFIG_MASK_OVERRIDE)) {
                    NativeImage img = NativeImage.read(in);
                    if (img.getWidth() != MODEL_W || img.getHeight() != MODEL_H) {
                        img = resize(img, MODEL_W, MODEL_H);
                    }
                    FluidCows.LOGGER.debug("Using mask override: {}", CONFIG_MASK_OVERRIDE);
                    return img;
                }
            }
        } catch (Throwable t) {
            FluidCows.LOGGER.warn("Failed reading mask override: {}", t.getMessage());
        }

        try {
            Optional<Resource> res = rm.getResource(ASSET_MASK_PREFERRED);
            if (res.isPresent()) {
                try (InputStream in = res.get().open()) {
                    NativeImage img = NativeImage.read(in);
                    if (img.getWidth() != MODEL_W || img.getHeight() != MODEL_H) {
                        img = resize(img, MODEL_W, MODEL_H);
                    }
                    FluidCows.LOGGER.debug("Using asset mask: {}", ASSET_MASK_PREFERRED);
                    return img;
                }
            }
        } catch (Throwable ignored) {}

        try {
            Optional<Resource> res = rm.getResource(ASSET_MASK_FALLBACK);
            if (res.isPresent()) {
                try (InputStream in = res.get().open()) {
                    NativeImage img = NativeImage.read(in);
                    if (img.getWidth() != MODEL_W || img.getHeight() != MODEL_H) {
                        img = resize(img, MODEL_W, MODEL_H);
                    }
                    FluidCows.LOGGER.debug("Using fallback asset mask: {}", ASSET_MASK_FALLBACK);
                    return img;
                }
            }
        } catch (Throwable ignored) {}

        FluidCows.LOGGER.warn("No cow white mask found (config or assets)");
        return null;
    }

    private static Path ensureConfigTile(Fluid fluid) throws IOException {
        Minecraft mc = Minecraft.getInstance();
        ResourceManager rm = mc.getResourceManager();
        ResourceLocation still = IClientFluidTypeExtensions.of(fluid).getStillTexture(new FluidStack(fluid, 1000));
        if (still == null) {
            throw new IOException("No still texture for " + BuiltInRegistries.FLUID.getKey(fluid));
        }

        String ns = still.getNamespace();
        String raw = still.getPath();
        String base = raw.startsWith("textures/") ? raw.substring("textures/".length()) : raw;
        if (base.endsWith(".png")) base = base.substring(0, base.length() - 4);
        Path outPath = CONFIG_TILE_ROOT.resolve(Paths.get(ns, base + ".png"));
        Files.createDirectories(outPath.getParent());

        if (Files.exists(outPath) && Files.size(outPath) > 0) return outPath;

        ResourceLocation[] cands = new ResourceLocation[]{
                still,
                ResourceLocation.fromNamespaceAndPath(ns, raw.endsWith(".png") ? raw : raw + ".png"),
                ResourceLocation.fromNamespaceAndPath(ns, raw.startsWith("textures/") ? raw : "textures/" + raw),
                ResourceLocation.fromNamespaceAndPath(ns, (raw.startsWith("textures/") ? raw : "textures/" + raw) + (raw.endsWith(".png") ? "" : ".png"))
        };
        Optional<Resource> res = Optional.empty();
        for (ResourceLocation rl : cands) {
            res = rm.getResource(rl);
            if (res.isPresent()) break;
        }
        if (res.isEmpty()) {
            throw new IOException("Could not open fluid texture resource: " + still);
        }

        try (InputStream in = res.get().open(); OutputStream out = Files.newOutputStream(outPath)) {
            in.transferTo(out);
        }
        return outPath;
    }

    private static ResourceLocation composeOverlay(ResourceLocation fkey, Fluid fluid) {
        try {
            Path tilePath = ensureConfigTile(fluid);
            Minecraft mc = Minecraft.getInstance();
            ResourceManager rm = mc.getResourceManager();

            NativeImage mask = loadMask(rm);
            if (mask == null) return ASSET_MASK_PREFERRED;
            NativeImage tile = readImage(tilePath);
            if (tile == null) return ASSET_MASK_PREFERRED;

            int tintARGB = IClientFluidTypeExtensions.of(fluid).getTintColor(new FluidStack(fluid, 1000));
            int tintR = (tintARGB >> 16) & 0xFF;
            int tintG = (tintARGB >> 8) & 0xFF;
            int tintB = tintARGB & 0xFF;

            int w = mask.getWidth(), h = mask.getHeight();
            int tw = tile.getWidth(), th = tile.getHeight();
            NativeImage out = new NativeImage(w, h, false);

            long alphaCount = 0;
            for (int y = 0; y < h; y++) {
                int ty = FLIP_SAMPLE_Y ? (th - 1 - (y % th)) : (y % th);
                for (int x = 0; x < w; x++) {
                    int m = mask.getPixelRGBA(x, y);
                    int a = (m >>> 24) & 0xFF;
                    if (a == 0) {
                        out.setPixelRGBA(x, y, 0);
                        continue;
                    }

                    int t = tile.getPixelRGBA(x % tw, ty);
                    int tr = (t) & 0xFF;
                    int tg = (t >>> 8) & 0xFF;
                    int tb = (t >>> 16) & 0xFF;

                    int r = (tr * tintR) / 255;
                    int g = (tg * tintG) / 255;
                    int b = (tb * tintB) / 255;

                    int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                    out.setPixelRGBA(x, y, abgr);
                    alphaCount++;
                }
            }

            Files.createDirectories(CONFIG_MASK_ROOT.resolve(fkey.getNamespace()));
            Path maskOut = CONFIG_MASK_ROOT.resolve(Paths.get(fkey.getNamespace(), fkey.getPath() + ".png"));
            out.writeToFile(maskOut);

            ResourceLocation dynRL = generatedRLFor(fkey.getNamespace(), fkey.getPath());
            FluidCows.LOGGER.debug("Composed overlay for {} (alphaPixels={}) -> {}", fkey, alphaCount, dynRL);
            mc.getTextureManager().register(dynRL, new DynamicTexture(out));
            mc.getTextureManager().getTexture(dynRL);
            return dynRL;
        } catch (Throwable t) {
            FluidCows.LOGGER.error("Failed to compose overlay for {}", fkey, t);
            return ASSET_MASK_PREFERRED;
        }
    }

    private static ResourceLocation generatedRLFor(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath("fluidcows", "generated/cow_mask/" + namespace + "/" + path);
    }

    private static NativeImage resize(NativeImage src, int w, int h) {
        NativeImage dst = new NativeImage(w, h, false);
        int sw = src.getWidth(), sh = src.getHeight();
        for (int y = 0; y < h; y++) {
            int sy = (int) Math.floor((y + 0.5) * sh / (double) h);
            if (sy >= sh) sy = sh - 1;
            for (int x = 0; x < w; x++) {
                int sx = (int) Math.floor((x + 0.5) * sw / (double) w);
                if (sx >= sw) sx = sw - 1;
                dst.setPixelRGBA(x, y, src.getPixelRGBA(sx, sy));
            }
        }
        return dst;
    }

    private static NativeImage readImage(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            return NativeImage.read(in);
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean isGoodFile(Path p) {
        try {
            return Files.exists(p) && Files.size(p) > 0;
        } catch (IOException e) {
            return false;
        }
    }
}
