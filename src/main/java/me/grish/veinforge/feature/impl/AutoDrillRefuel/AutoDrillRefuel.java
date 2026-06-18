package me.grish.veinforge.feature.impl.AutoDrillRefuel;

import lombok.Getter;
import lombok.Setter;
import me.grish.veinforge.failsafe.AbstractFailsafe.Failsafe;
import me.grish.veinforge.feature.AbstractFeature;
import me.grish.veinforge.feature.impl.AutoDrillRefuel.states.AutoDrillRefuelState;
import me.grish.veinforge.feature.impl.AutoDrillRefuel.states.StartingState;

public class AutoDrillRefuel extends AbstractFeature {
    @Getter
    private static final AutoDrillRefuel instance = new AutoDrillRefuel();
    @Setter
    @Getter
    private AutoDrillRefuelError error = AutoDrillRefuelError.NONE;
    @Getter
    private FuelType fuelType;
    @Getter
    private String drillName;
    private AutoDrillRefuelState currentState;

    @Override
    public String getName() {
        return "AutoDrillRefuel";
    }

    @Override
    public void resetStatesAfterStop() {
        this.failsafesToIgnore.remove(Failsafe.ITEM_CHANGE);
    }

    public void start(String drillName, FuelType fuelType) {
        this.enabled = true;
        this.drillName = drillName;
        this.fuelType = fuelType;
        this.error = AutoDrillRefuelError.NONE;
        currentState = new StartingState();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    protected void onTick() {
        if (!this.enabled) {
            return;
        }

        if (currentState == null)
            return;

        AutoDrillRefuelState nextState = currentState.onTick(this);
        transitionTo(nextState);
    }

    private void transitionTo(AutoDrillRefuelState nextState) {
        // Skip if no state change
        if (currentState == nextState)
            return;

        currentState.onEnd(this);
        currentState = nextState;

        if (currentState == null) {
            log("null state, returning");
            return;
        }

        currentState.onStart(this);
    }

    public enum AutoDrillRefuelError {
        NONE,
        NO_DRILL,
        NO_FUEL,
        NO_ABIPHONE,
        NO_GREATFORGE_CONTACT
    }

    public enum FuelType {
        VOLTA("Volta"),
        OIL_BARREL("Oil Barrel"),
        SUNFLOWER_OIL("Sunflower Oil");

        @Getter
        private final String name;

        FuelType(String name) {
            this.name = name;
        }
    }

}
