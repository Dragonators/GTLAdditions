package com.gtladd.gtladditions.common.machine.muiltblock;

import org.gtlcore.gtlcore.api.machine.multiblock.GTLCleanroomType;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.machine.feature.ICleanroomProvider;
import com.gregtechceu.gtceu.api.machine.multiblock.CleanroomType;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Set;

public class GTLAddMultiblockDisplayTextBuilder {

    public static Builder builder(List<Component> textList, boolean isStructureFormed) {
        return new Builder(textList, isStructureFormed, true);
    }

    public static class Builder {

        private final List<Component> textList;
        private final boolean isStructureFormed;
        private boolean isWorkingEnabled;
        private boolean isActive;

        private Builder(List<Component> textList, boolean isStructureFormed, boolean showIncompleteStructureWarning) {
            this.textList = textList;
            this.isStructureFormed = isStructureFormed;
            if (!isStructureFormed && showIncompleteStructureWarning) {
                MutableComponent base = Component.translatable("gtceu.multiblock.invalid_structure").withStyle(ChatFormatting.RED);
                Component hover = Component.translatable("gtceu.multiblock.invalid_structure.tooltip").withStyle(ChatFormatting.GRAY);
                textList.add(base.withStyle((style) -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))));
            }
        }

        public Builder setWorkingStatus(boolean isWorkingEnabled, boolean isActive) {
            this.isWorkingEnabled = isWorkingEnabled;
            this.isActive = isActive;
            return this;
        }

        public Builder addEnergyUsageLine(IEnergyContainer energyContainer) {
            if (!this.isStructureFormed) return this;
            else {
                if (energyContainer != null && energyContainer.getEnergyCapacity() > 0L) {
                    long maxVoltage = Math.max(energyContainer.getInputVoltage(), energyContainer.getOutputVoltage());
                    String energyFormatted = FormattingUtil.formatNumbers(maxVoltage);
                    Component voltageName = Component.literal(GTValues.VNF[GTUtil.getFloorTierByVoltage(maxVoltage)]);
                    MutableComponent bodyText = Component.translatable("gtceu.multiblock.max_energy_per_tick", energyFormatted, voltageName).withStyle(ChatFormatting.GRAY);
                    Component hoverText = Component.translatable("gtceu.multiblock.max_energy_per_tick_hover").withStyle(ChatFormatting.GRAY);
                    this.textList.add(bodyText.withStyle((style) -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))));
                }
                return this;
            }
        }

        public Builder addEnergyTierLine(int tier) {
            if (!this.isStructureFormed) return this;
            else if (tier >= 0 && tier <= 14) {
                Component voltageName = Component.literal(GTValues.VNF[tier]);
                MutableComponent bodyText = Component.translatable("gtceu.multiblock.max_recipe_tier", voltageName).withStyle(ChatFormatting.GRAY);
                Component hoverText = Component.translatable("gtceu.multiblock.max_recipe_tier_hover").withStyle(ChatFormatting.GRAY);
                this.textList.add(bodyText.withStyle((style) -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))));
                return this;
            } else return this;
        }

        public Builder addMachineModeLine(GTRecipeType recipeType) {
            if (!this.isStructureFormed) return this;
            else {
                this.textList.add(Component.translatable("gtceu.gui.machinemode", Component.translatable(recipeType.registryName.toLanguageKey())).withStyle(ChatFormatting.AQUA));
                return this;
            }
        }

        public Builder addParallelsLine(int numParallels) {
            if (!this.isStructureFormed) return this;
            else {
                if (numParallels > 1) {
                    Component parallels = Component.literal(FormattingUtil.formatNumbers(numParallels)).withStyle(ChatFormatting.DARK_PURPLE);
                    this.textList.add(Component.translatable("gtceu.multiblock.parallel", parallels).withStyle(ChatFormatting.GRAY));
                }
                return this;
            }
        }

        public Builder addWorkingStatusLine() {
            if (!this.isStructureFormed) return this;
            else if (!this.isWorkingEnabled) return this.addWorkPausedLine(false);
            else return this.isActive ? this.addRunningPerfectlyLine(false) : this.addIdlingLine(false);
        }

        public Builder addWorkPausedLine(boolean checkState) {
            if (!this.isStructureFormed) return this;
            else {
                if (!checkState || !this.isWorkingEnabled) {
                    this.textList.add(Component.translatable("gtceu.multiblock.work_paused").withStyle(ChatFormatting.GOLD));
                }
                return this;
            }
        }

        public Builder addRunningPerfectlyLine(boolean checkState) {
            if (!this.isStructureFormed) return this;
            else {
                if (!checkState || this.isActive) {
                    this.textList.add(Component.translatable("gtceu.multiblock.running").withStyle(ChatFormatting.GREEN));
                }
                return this;
            }
        }

        public Builder addIdlingLine(boolean checkState) {
            if (!this.isStructureFormed) return this;
            else {
                if (!checkState || this.isWorkingEnabled && !this.isActive) {
                    this.textList.add(Component.translatable("gtceu.multiblock.idling").withStyle(ChatFormatting.GRAY));
                }
                return this;
            }
        }

        public Builder addProgressLine(double progressPercent) {
            if (this.isStructureFormed && this.isActive) {
                int currentProgress = (int) (progressPercent * 100.0);
                this.textList.add(Component.translatable("gtceu.multiblock.progress", currentProgress));
                return this;
            } else return this;
        }

        public Builder addMaintenanceTierLines(ICleanroomProvider cleanroomTypes) {
            if (!this.isStructureFormed || !ConfigHolder.INSTANCE.machines.enableMaintenance) return this;
            else {
                Component cleanroomType = Component.translatable("gtceu.io.none").withStyle(ChatFormatting.RED);
                if (cleanroomTypes != null) {
                    if (cleanroomTypes.isClean()) {
                        Set<CleanroomType> cleaningRooms = cleanroomTypes.getTypes();
                        if (cleaningRooms.contains(GTLCleanroomType.LAW_CLEANROOM)) {
                            cleanroomType = Component.translatable(GTLCleanroomType.LAW_CLEANROOM.getTranslationKey()).withStyle(ChatFormatting.RED);
                        } else if (cleaningRooms.contains(CleanroomType.STERILE_CLEANROOM)) {
                            cleanroomType = Component.translatable(CleanroomType.STERILE_CLEANROOM.getTranslationKey()).withStyle(ChatFormatting.RED);
                        } else if (cleaningRooms.contains(CleanroomType.CLEANROOM)) {
                            cleanroomType = Component.translatable(CleanroomType.CLEANROOM.getTranslationKey()).withStyle(ChatFormatting.RED);
                        }
                    } else cleanroomType = Component.translatable("gtceu.io.none").withStyle(ChatFormatting.RED);
                }
                this.textList.add(Component.translatable("gtceu.multiblock.cleanroom_tier", cleanroomType).withStyle(ChatFormatting.WHITE));
                return this;
            }
        }

        public Builder addGravityLine(int hasGravity) {
            if (!this.isStructureFormed) return this;
            else {
                Component gravity;
                if (hasGravity == 0) {
                    gravity = Component.translatable("gtceu.multiblock.gravity_none").withStyle(ChatFormatting.RED);
                } else if (hasGravity == 100) {
                    gravity = Component.translatable("gtceu.multiblock.gravity_strong").withStyle(ChatFormatting.RED);
                } else {
                    this.textList.add(Component.translatable("gtceu.multiblock.gravity_partial").withStyle(ChatFormatting.WHITE));
                    return this;
                }
                this.textList.add(Component.translatable("gtceu.multiblock.gravity_tier", gravity).withStyle(ChatFormatting.WHITE));
                return this;
            }
        }
    }
}
