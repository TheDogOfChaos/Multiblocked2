package com.lowdragmc.mbd2.common.trait.entity;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.ingredient.EntityIngredient;
import com.lowdragmc.mbd2.common.capability.recipe.EntityRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.RecipeCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.RecipeHandlerTrait;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class EntityHandlerTrait extends RecipeCapabilityTrait {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(EntityHandlerTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    private final List<Entity> entities = new ArrayList<>();
    private final Lock entitiesLock = new ReentrantLock();
    private final EntityRecipeHandler handler = new EntityRecipeHandler();

    public EntityHandlerTrait(MBDMachine machine, EntityHandlerTraitDefinition definition) {
        super(machine, definition);
    }

    @Override
    public EntityHandlerTraitDefinition getDefinition() {
        return (EntityHandlerTraitDefinition) super.getDefinition();
    }

    @Override
    public void serverTick() {
        if (getHandlerIO() == IO.IN && getMachine().getOffsetTimer() % 20 == 0) {
            if (entitiesLock.tryLock()) {
                var area = getDefinition().getArea(getMachine().getFrontFacing().orElse(null));
                area = area.move(getMachine().getPos());
                var detected = getMachine().getLevel().getEntities((Entity)null, area, Entity::isAlive);
                if (detected.size() != entities.size() || !new HashSet<>(detected).containsAll(entities)) {
                    entities.clear();
                    entities.addAll(detected);
                    notifyListeners();
                }
                entitiesLock.unlock();
            }
        }
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(handler);
    }

    public class EntityRecipeHandler extends RecipeHandlerTrait<EntityIngredient> {

        protected EntityRecipeHandler() {
            super(EntityHandlerTrait.this, EntityRecipeCapability.CAP);
        }

        @Override
        public List<EntityIngredient> handleRecipeInner(IO io, MBDRecipe recipe, List<EntityIngredient> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            if (io == IO.OUT) {
                if (!simulate && getMachine().getLevel() instanceof ServerLevel serverLevel) {
                    // spawn entities
                    var area = getDefinition().getArea(getMachine().getFrontFacing().orElse(null));
                    area = area.move(getMachine().getPos());
                    for (EntityIngredient entityIngredient : left) {
                        for (EntityType<?> type : entityIngredient.getTypes()) {
                            var pos = new Vec3((area.minX + Math.random() * area.getXsize()),
                                    (area.minY + Math.random() * area.getYsize()),
                                    (area.minZ + Math.random() * area.getZsize()));
                            var entity = type.spawn(serverLevel, new BlockPos((int) pos.x, (int) pos.y, (int) pos.z), MobSpawnType.SPAWNER);
                            if (entity != null) {
                                if (entityIngredient.getNbt() != null) {
                                    var tag = entity.serializeNBT();
                                    tag.merge(entityIngredient.getNbt());
                                    entity.load(tag);
                                }
                                entity.moveTo(pos);
                            }
                        }
                    }
                }
                return null;
            } else if (io == IO.IN) {
                entitiesLock.lock();
                var entityList = simulate ? new ArrayList<>(entities) : entities;
                entitiesLock.unlock();
                var iterator = left.iterator();
                while (iterator.hasNext()) {
                    var ingredient = iterator.next();
                    var types = Arrays.stream(ingredient.getTypes()).collect(Collectors.toSet());
                    var toKilled = new ArrayList<Entity>();
                    var matchCount = 0;
                    for (var entity : entityList) {
                        if (matchCount >= ingredient.getCount()) {
                            break;
                        }
                        if (entity.isAlive() && types.contains(entity.getType())) {
                            var nbt = ingredient.getNbt();
                            if (nbt != null && !nbt.isEmpty()) {
                                var held = entity.serializeNBT();
                                var copied = nbt.copy();
                                copied.merge(held);
                                if (!nbt.equals(copied)) {
                                    continue;
                                }
                            }
                            matchCount++;
                            toKilled.add(entity);
                        }
                    }
                    ingredient.setCount(ingredient.getCount() - matchCount);
                    if (ingredient.getCount() <= 0) {
                        iterator.remove();
                    }
                    if (!simulate) {
                        toKilled.forEach(entity -> entity.remove(Entity.RemovalReason.DISCARDED));
                    }
                    entityList.removeAll(toKilled);
                }
            }
            return left.isEmpty() ? null : left;
        }
    }
}
