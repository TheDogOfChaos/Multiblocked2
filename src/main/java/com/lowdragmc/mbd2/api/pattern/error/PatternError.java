package com.lowdragmc.mbd2.api.pattern.error;

import com.lowdragmc.mbd2.api.pattern.MultiblockState;
import com.lowdragmc.mbd2.api.pattern.TraceabilityPredicate;
import com.lowdragmc.mbd2.api.pattern.predicates.SimplePredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class PatternError {

    protected MultiblockState worldState;

    public void setWorldState(MultiblockState worldState) {
        this.worldState = worldState;
    }

    public Level getWorld() {
        return worldState.getWorld();
    }

    public BlockPos getPos() {
        return worldState.getPos();
    }

    public List<List<ItemStack>> getCandidates() {
        TraceabilityPredicate predicate = worldState.predicate;
        List<List<ItemStack>> candidates = new ArrayList<>();
        for (SimplePredicate common : predicate.common) {
            candidates.add(common.getCandidates());
        }
        for (SimplePredicate limited : predicate.limited) {
            candidates.add(limited.getCandidates());
        }
        return candidates;
    }

    public Component getErrorInfo() {
        List<List<ItemStack>> candidates = getCandidates();
        var items = Component.empty();
        for (List<ItemStack> candidate : candidates) {
            if (!candidate.isEmpty()) {
                items.append(candidate.get(0).getDisplayName());
                items.append(Component.literal(", "));
            }
        }
        return Component.translatable("mbd2.multiblock.pattern.error", items, worldState.getPos());
    }
}
