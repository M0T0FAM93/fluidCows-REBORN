package motofam93.fluidcows.compat.jei;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import motofam93.fluidcows.FluidCowConfigGenerator;
import motofam93.fluidcows.item.FluidCowSpawnItem;
import motofam93.fluidcows.util.EnabledFluids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class JeiCowData {
    private JeiCowData() {}

    static List<JeiCowBreedingRecipe> buildBreedingRecipes() {
        List<JeiCowBreedingRecipe> list = new ArrayList<>();
        for (ResourceLocation child : EnabledFluids.all()) {
            var b = readBreeding(child);
            if (b == null) continue;
            if (b.parent1 == null || b.parent2 == null) continue;

            Optional<Item> optItem = BuiltInRegistries.ITEM.getOptional(b.breedingItem);
            if (optItem.isEmpty()) continue;

            ItemStack p1 = FluidCowSpawnItem.withFluid(b.parent1);
            ItemStack p2 = FluidCowSpawnItem.withFluid(b.parent2);
            ItemStack out = FluidCowSpawnItem.withFluid(child);
            ItemStack item = new ItemStack(optItem.get());

            list.add(new JeiCowBreedingRecipe(p1, p2, item, out, b.chance));
        }
        return list;
    }

    static List<JeiCowInfoRecipe> buildInfoRecipes() {
        List<JeiCowInfoRecipe> list = new ArrayList<>();
        for (ResourceLocation rl : EnabledFluids.all()) {
            int breed = EnabledFluids.getBreedingCooldown(rl);
            int grow  = EnabledFluids.getGrowthTimeTicks(rl);
            int milk  = EnabledFluids.getBucketCooldownTicks(rl);
            int weight= EnabledFluids.getWeight(rl);
            list.add(new JeiCowInfoRecipe(FluidCowSpawnItem.withFluid(rl), breed, grow, milk, weight));
        }
        return list;
    }

    static Breeding readBreeding(ResourceLocation child) {
        Path root = FluidCowConfigGenerator.configRoot();
        Path json = root.resolve(child.getNamespace()).resolve(child.getPath() + ".json");
        if (!Files.exists(json)) return null;

        try (Reader r = Files.newBufferedReader(json)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            if (!obj.has("breeding")) return null;
            JsonObject b = obj.getAsJsonObject("breeding");

            String itemId = b.has("breeding_item") ? b.get("breeding_item").getAsString() : "minecraft:wheat";
            ResourceLocation itemRl = ResourceLocation.tryParse(itemId);

            String p1s = b.has("parent_1") ? b.get("parent_1").getAsString() : "";
            String p2s = b.has("parent_2") ? b.get("parent_2").getAsString() : "";
            int chance = b.has("chance") ? b.get("chance").getAsInt() : 33;

            Breeding res = new Breeding();
            res.breedingItem = itemRl;
            res.parent1 = p1s.isEmpty() ? null : ResourceLocation.tryParse(p1s);
            res.parent2 = p2s.isEmpty() ? null : ResourceLocation.tryParse(p2s);
            res.chance = chance;
            return res;
        } catch (IOException ignored) {
            return null;
        }
    }

    static final class Breeding {
        ResourceLocation breedingItem;
        ResourceLocation parent1, parent2;
        int chance;
    }
}
