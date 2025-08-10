package com.gtladd.gtladditions.api.machine.trait;

import org.gtlcore.gtlcore.api.machine.trait.IMERecipeHandlerTrait;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;

import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

public abstract class NotifiableMERecipeHandlerTrait<T> extends MachineTrait implements IMERecipeHandlerTrait<T> {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(NotifiableMERecipeHandlerTrait.class);
    protected List<Runnable> listeners = new ObjectArrayList<>();

    public NotifiableMERecipeHandlerTrait(MetaMachine machine) {
        super(machine);
    }

    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public ISubscription addChangedListener(Runnable listener) {
        this.listeners.add(listener);
        return () -> this.listeners.remove(listener);
    }

    public void notifyListeners() {
        this.listeners.forEach(Runnable::run);
    }
}
