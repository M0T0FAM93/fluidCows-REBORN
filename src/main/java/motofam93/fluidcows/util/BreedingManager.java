package motofam93.fluidcows.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import motofam93.fluidcows.FluidCowConfigGenerator;
import motofam93.fluidcows.FluidCows;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class BreedingManager {
    private BreedingManager() {}

    public record Rule(ResourceLocation child, ResourceLocation p1, ResourceLocation p2, Item item, int chance) {}

    private static final class ParentKey {
        final ResourceLocation a, b;

        ParentKey(ResourceLocation x, ResourceLocation y) {
            if (x.toString().compareTo(y.toString()) <= 0) { a = x; b = y; }
            else { a = y; b = x; }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ParentKey pk)) return false;
            return a.equals(pk.a) && b.equals(pk.b);
        }

        @Override
        public int hashCode() { return 31 * a.hashCode() + b.hashCode(); }
    }

    private static final Map<ParentKey, Rule> RULES = new HashMap<>();
    private static final Map<ResourceLocation, Set<Item>> ITEMS_BY_PARENT = new HashMap<>();

    public static void reload() {
        RULES.clear();
        ITEMS_BY_PARENT.clear();

        Path root = FluidCowConfigGenerator.configRoot();
        if (!Files.isDirectory(root)) return;

        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(BreedingManager::readOne);
        } catch (IOException e) {
            FluidCows.LOGGER.error("Failed to reload breeding rules: {}", e.getMessage());
        }
    }

    private static void readOne(Path file) {
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            if (!obj.has("breeding") || !obj.get("breeding").isJsonObject()) return;

            String name = file.getFileName().toString().replace(".json", "");
            String ns = file.getParent().getFileName().toString();
            ResourceLocation child = ResourceLocation.fromNamespaceAndPath(ns, name);

            JsonObject b = obj.getAsJsonObject("breeding");

            ResourceLocation p1 = parseRL(b, "parent_1");
            ResourceLocation p2 = parseRL(b, "parent_2");
            if (p1 == null || p2 == null) return;

            ResourceLocation itemRL = parseRL(b, "breeding_item");
            Item item = itemRL != null ? BuiltInRegistries.ITEM.get(itemRL) : Items.WHEAT;
            if (item == null) item = Items.WHEAT;

            int chance = b.has("chance") ? Math.max(0, Math.min(100, b.get("chance").getAsInt())) : 100;

            RULES.put(new ParentKey(p1, p2), new Rule(child, p1, p2, item, chance));
            ITEMS_BY_PARENT.computeIfAbsent(p1, k -> new ObjectOpenHashSet<>()).add(item);
            ITEMS_BY_PARENT.computeIfAbsent(p2, k -> new ObjectOpenHashSet<>()).add(item);
        } catch (Throwable t) {
            FluidCows.LOGGER.error("Failed to read breeding config {}: {}", file, t.getMessage());
        }
    }

    private static ResourceLocation parseRL(JsonObject json, String key) {
        if (!json.has(key)) return null;
        try { return ResourceLocation.parse(json.get(key).getAsString()); }
        catch (Throwable t) { return null; }
    }

    public static Rule findRule(ResourceLocation a, ResourceLocation b) {
        return RULES.get(new ParentKey(a, b));
    }

    public static boolean isBreedingItemForParent(ResourceLocation parentFluid, ItemStack stack) {
        Set<Item> set = ITEMS_BY_PARENT.get(parentFluid);
        if (set != null && !set.isEmpty()) {
            for (Item i : set) if (stack.is(i)) return true;
            return false;
        }
        return stack.is(Items.WHEAT);
    }

    public static Ingredient ingredientForParent(ResourceLocation parentFluid) {
        Set<Item> set = ITEMS_BY_PARENT.get(parentFluid);
        if (set != null && !set.isEmpty()) {
            return Ingredient.of(set.stream().map(ItemStack::new).toArray(ItemStack[]::new));
        }
        return Ingredient.of(Items.WHEAT);
    }
}
