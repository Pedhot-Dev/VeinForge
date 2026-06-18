package me.grish.veinforge.feature.impl;

import lombok.Getter;
import me.grish.veinforge.feature.AbstractFeature;

public class WorldScanner extends AbstractFeature {

    @Getter
    public static WorldScanner instance = new WorldScanner();

    @Override
    public String getName() {
        return "WorldScanner";
    }

}
