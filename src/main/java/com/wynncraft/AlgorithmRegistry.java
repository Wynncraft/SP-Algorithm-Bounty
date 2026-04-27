package com.wynncraft;


import com.wynncraft.algorithms.NegativeOrderAlgorithm;
import com.wynncraft.algorithms.CapyTopoAlgorithm;
import com.wynncraft.algorithms.PrunedMaskAlgorithm;
import com.wynncraft.algorithms.PrunedMaskV2Algorithm;
import com.wynncraft.algorithms.WynnFrumaAlgorithm;
import com.wynncraft.core.WynnPlayer;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IPlayerBuilder;
import com.wynncraft.core.interfaces.Information;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AlgorithmRegistry {

    private static final List<Entry> registry = new ArrayList<>();
    static {
        // Register here your algorithm here and your custom player if necessary!
        // Make sure your algorithm contains the @Information annotation
        // New additions always goes on the bottom for reference
        register(new WynnFrumaAlgorithm(), WynnPlayer.Builder::new);
        register(new CapyTopoAlgorithm(), WynnPlayer.Builder::new);
        register(new NegativeOrderAlgorithm(), WynnPlayer.Builder::new);
        register(new PrunedMaskAlgorithm(), WynnPlayer.Builder::new);
        register(new PrunedMaskV2Algorithm(), WynnPlayer.Builder::new);
    }

    /**
     * Register a new algorithm we will run tests against
     *
     * Make sure your algorithm class has the {@link Information} annotation
     * otherwise it won't be registered
     *
     * @param algorithm the algorithm instance
     * @param playerBuilder the player builder to use
     */
    protected static void register(IAlgorithm algorithm, Supplier<IPlayerBuilder> playerBuilder) {
        Information information = algorithm.getClass().getAnnotation(Information.class);
        if (information == null) {
            throw new IllegalArgumentException("Algorithm class " + algorithm.getClass().getName() + " must have @Information annotation");
        }

        registry.add(new Entry(information, algorithm, playerBuilder));
    }

    /**
     * @return all current registered algorithms
     */
    public static List<Entry> registry() {
        return registry;
    }

    /**
     * Represents a registered algorithm that
     * we will run tests for
     *
     * @param information the algorithm information
     * @param algorithm the algorithm instance itself
     * @param playerBuilder the player builder supplier
     */
    public record Entry(Information information, IAlgorithm algorithm, Supplier<IPlayerBuilder> playerBuilder) {

        /**
         * @return the name of the algorithm
         */
        public String name() {
            return information.name() + " V" + information.version();
        }

        /**
         * @return the author of the algorithm
         */
        public String[] authors() {
            return information.authors();
        }

        /**
         * @return creates a new player builder
         */
        public IPlayerBuilder builder() {
            return playerBuilder.get();
        }

    }

}
