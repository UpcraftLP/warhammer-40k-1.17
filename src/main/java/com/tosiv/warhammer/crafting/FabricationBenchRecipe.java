package com.tosiv.warhammer.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.tosiv.warhammer.util.registry.ModBlocks;
import com.tosiv.warhammer.util.registry.ModRecipes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class FabricationBenchRecipe implements Recipe<PlayerInventory> {

    private static final int MAX_ALLOWED_ITEMS = 5;

    private final Identifier id;
    private final ItemStack result;
    private final DefaultedList<Pair<Ingredient, Integer>> input;

    public FabricationBenchRecipe(Identifier id, ItemStack result, DefaultedList<Pair<Ingredient, Integer>> input) {
        this.id = id;
        this.result = result;
        this.input = input;
    }

    @Override
    public boolean matches(PlayerInventory inventory, World world) {
        return false;
    }

    @Override
    public ItemStack craft(PlayerInventory inventory) {
        return null;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= input.size();
    }

    @Override
    public ItemStack getOutput() {
        return result;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FABRICATION_BENCH_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.FABRICATION_BENCH;
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(ModBlocks.IMPERIAL_FABRICATION_BENCH);
    }

    public static class Serializer implements RecipeSerializer<FabricationBenchRecipe> {

        @Override
        public FabricationBenchRecipe read(Identifier id, JsonObject json) {
            JsonArray ingredients = JsonHelper.getArray(json, "ingredients");
            DefaultedList<Pair<Ingredient, Integer>> input = DefaultedList.of();

            for (int i = 0; i < ingredients.size(); i++) {
                JsonObject element = JsonHelper.asObject(ingredients.get(i), "ingredients[" + i + "]");
                Ingredient ingredient = Ingredient.fromJson(element);
                int amount = JsonHelper.getInt(element, "count");
                if (!ingredient.isEmpty() && amount > 0) {
                    input.add(new Pair<>(ingredient, amount));
                }
            }
            if (input.isEmpty()) {
                throw new JsonParseException("No ingredients for recipe");
            } else if (input.size() > MAX_ALLOWED_ITEMS) {
                throw new JsonParseException("Too many ingredients for recipe");
            } else {
                ItemStack result = ShapedRecipe.outputFromJson(JsonHelper.getObject(json, "result"));
                return new FabricationBenchRecipe(id, result, input);
            }
        }

        @Override
        public FabricationBenchRecipe read(Identifier id, PacketByteBuf buf) {
            int size = buf.readVarInt();
            DefaultedList<Pair<Ingredient, Integer>> input = DefaultedList.ofSize(size, new Pair<>(Ingredient.EMPTY, 0));

            for (int j = 0; j < input.size(); ++j) {
                Ingredient ingredient = Ingredient.fromPacket(buf);
                int amount = buf.readVarInt();
                input.set(j, new Pair<>(ingredient, amount));
            }

            ItemStack result = buf.readItemStack();
            return new FabricationBenchRecipe(id, result, input);
        }

        @Override
        public void write(PacketByteBuf buf, FabricationBenchRecipe recipe) {
            buf.writeVarInt(recipe.input.size());
            for (Pair<Ingredient, Integer> entry : recipe.input) {
                entry.getLeft().write(buf);
                buf.writeVarInt(entry.getRight());
            }
            buf.writeItemStack(recipe.result);
        }
    }
}
