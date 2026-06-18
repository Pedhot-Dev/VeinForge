package me.grish.veinforge.feature.impl;

import lombok.Getter;
import me.grish.veinforge.feature.AbstractFeature;

public class AutoSell extends AbstractFeature {

    @Getter
    public static AutoSell instance = new AutoSell();

    @Override
    public String getName() {
        return "AutoSell";
    }

}
