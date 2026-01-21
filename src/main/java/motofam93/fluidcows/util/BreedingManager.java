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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class BreedingManager {
    private BreedingManager() {}

    public record Rule(ResourceLocation child, ResourceLocation p1, ResourceLocation p2, Item item, int chance) {}

    private static final Map<ResourceLocation, Set<Item>> ITEMS_BY_PARENT = new HashMap<>();

    public static void reload() {
        ITEMS_BY_PARENT.clear();

        Path root = FluidCowConfigGenerator.configRoot();
        if (!Files.isDirectory(root)) return;

        try (Stream<Path> files = Files.walk(root)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(BreedingManager::cacheBreedingItem);
        } catch (IOException e) {
            FluidCows.LOGGER.error("Failed to reload breeding rules: {}", e.getMessage());
        }
    }

    private static void cacheBreedingItem(Path file) {
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            if (!obj.has("breeding") || !obj.get("breeding").isJsonObject()) return;

            JsonObject b = obj.getAsJsonObject("breeding");

            ResourceLocation p1 = parseRL(b, "parent_1");
            ResourceLocation p2 = parseRL(b, "parent_2");
            if (p1 == null || p2 == null) return;

            ResourceLocation itemRL = parseRL(b, "breeding_item");
            Item item = itemRL != null ? BuiltInRegistries.ITEM.get(itemRL) : Items.WHEAT;
            if (item == null || item == Items.AIR) item = Items.WHEAT;

            ITEMS_BY_PARENT.computeIfAbsent(p1, k -> new ObjectOpenHashSet<>()).add(item);
            ITEMS_BY_PARENT.computeIfAbsent(p2, k -> new ObjectOpenHashSet<>()).add(item);
        } catch (Throwable ignored) {}
    }

    private static ResourceLocation parseRL(JsonObject json, String key) {
        if (!json.has(key)) return null;
        String val = json.get(key).getAsString();
        if (val == null || val.isEmpty()) return null;
        try { return ResourceLocation.parse(val); }
        catch (Throwable t) { return null; }
    }

    public static Rule findRule(ResourceLocation a, ResourceLocation b) {
        Path root = FluidCowConfigGenerator.configRoot();
        if (!Files.isDirectory(root)) return null;

        List<Rule> matchingRules = new ArrayList<>();

        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                Rule rule = readRuleFromFile(file, a, b);
                if (rule != null) matchingRules.add(rule);
            }
        } catch (IOException ignored) {}

        if (matchingRules.isEmpty()) return null;
        if (matchingRules.size() == 1) return matchingRules.get(0);

        int totalChance = 0;
        for (Rule r : matchingRules) totalChance += r.chance();
        if (totalChance <= 0) return matchingRules.get(0);

        int roll = (int) (Math.random() * totalChance);
        int acc = 0;
        for (Rule r : matchingRules) {
            acc += r.chance();
            if (roll < acc) return r;
        }
        return matchingRules.get(0);
    }

    private static Rule readRuleFromFile(Path file, ResourceLocation a, ResourceLocation b) {
        if (!Files.exists(file)) return null;
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            if (!obj.has("breeding") || !obj.get("breeding").isJsonObject()) return null;

            String name = file.getFileName().toString().replace(".json", "");
            String ns = file.getParent().getFileName().toString();
            ResourceLocation child = ResourceLocation.fromNamespaceAndPath(ns, name);

            JsonObject br = obj.getAsJsonObject("breeding");
            ResourceLocation p1 = parseRL(br, "parent_1");
            ResourceLocation p2 = parseRL(br, "parent_2");

            if (p1 == null || p2 == null) return null;

            boolean matches = (p1.equals(a) && p2.equals(b)) || (p1.equals(b) && p2.equals(a));
            if (!matches) return null;

            ResourceLocation itemRL = parseRL(br, "breeding_item");
            Item item = itemRL != null ? BuiltInRegistries.ITEM.get(itemRL) : Items.WHEAT;
            if (item == null || item == Items.AIR) item = Items.WHEAT;

            int chance = br.has("chance") ? Math.max(0, Math.min(100, br.get("chance").getAsInt())) : 100;

            return new Rule(child, p1, p2, item, chance);
        } catch (Throwable t) {
            return null;
        }
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
