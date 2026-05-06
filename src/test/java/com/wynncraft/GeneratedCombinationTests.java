package com.wynncraft;

import com.wynncraft.core.CombinationTest;
import com.wynncraft.core.interfaces.IAlgorithm;
import com.wynncraft.core.interfaces.IPlayer;
import com.wynncraft.core.interfaces.IPlayerBuilder;
import com.wynncraft.enums.Equipment;
import com.wynncraft.enums.SkillPoint;
import org.junit.jupiter.api.Tag;

import static com.wynncraft.core.EquipmentAssertions.*;

@Tag("generated")
class GeneratedCombinationTests {

    @CombinationTest
    public void sugo001(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 73);
            builder.allocate(SkillPoint.INTELLIGENCE, 59);
            builder.allocate(SkillPoint.DEFENCE, 70);
            builder.equipment(
                Equipment.TITANOMACHIA,
                Equipment.EMPYREAL_EMBERPLATE,
                Equipment.CHAIN_RULE,
                Equipment.CRUSADE_SABATONS,
                Equipment.MOON_POOL_CIRCLET,
                Equipment.INTENSITY,
                Equipment.DRAGONS_EYE_BRACELET,
                Equipment.SIMULACRUM
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.TITANOMACHIA,
            Equipment.EMPYREAL_EMBERPLATE,
            Equipment.CHAIN_RULE,
            Equipment.CRUSADE_SABATONS,
            Equipment.MOON_POOL_CIRCLET,
            Equipment.INTENSITY,
            Equipment.DRAGONS_EYE_BRACELET,
            Equipment.SIMULACRUM
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo002(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 34);
            builder.allocate(SkillPoint.DEXTERITY, 42);
            builder.allocate(SkillPoint.INTELLIGENCE, 34);
            builder.allocate(SkillPoint.DEFENCE, 34);
            builder.allocate(SkillPoint.AGILITY, 57);
            builder.equipment(
                Equipment.LOGISTICS,
                Equipment.ETIOLATION,
                Equipment.RAINBOW_SANCTUARY,
                Equipment.VIRTUOSO,
                Equipment.PHOTON,
                Equipment.PHOTON,
                Equipment.PROWESS,
                Equipment.RENDA_LANGIT
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.LOGISTICS,
            Equipment.ETIOLATION,
            Equipment.RAINBOW_SANCTUARY,
            Equipment.VIRTUOSO,
            Equipment.PHOTON,
            Equipment.PHOTON,
            Equipment.PROWESS,
            Equipment.RENDA_LANGIT
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo003(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 57);
            builder.allocate(SkillPoint.DEXTERITY, 37);
            builder.allocate(SkillPoint.INTELLIGENCE, 27);
            builder.allocate(SkillPoint.DEFENCE, 11);
            builder.allocate(SkillPoint.AGILITY, 57);
            builder.equipment(
                Equipment.LOGISTICS,
                Equipment.DISCOVERER,
                Equipment.RINGLETS,
                Equipment.REVENANT,
                Equipment.PHOTON,
                Equipment.PHOTON,
                Equipment.PROWESS,
                Equipment.DIAMOND_FUSION_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.LOGISTICS,
            Equipment.DISCOVERER,
            Equipment.RINGLETS,
            Equipment.REVENANT,
            Equipment.PHOTON,
            Equipment.PHOTON,
            Equipment.PROWESS,
            Equipment.DIAMOND_FUSION_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo004(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 42);
            builder.allocate(SkillPoint.DEXTERITY, 92);
            builder.allocate(SkillPoint.INTELLIGENCE, 56);
            builder.equipment(
                Equipment.CAESURA,
                Equipment.DELIRIUM,
                Equipment.CHAOS_WOVEN_GREAVES,
                Equipment.STARDEW,
                Equipment.HYPOXIA,
                Equipment.YANG,
                Equipment.DIAMOND_STATIC_BRACELET,
                Equipment.LIGHTNING_FLASH
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.CAESURA,
            Equipment.DELIRIUM,
            Equipment.CHAOS_WOVEN_GREAVES,
            Equipment.STARDEW,
            Equipment.HYPOXIA,
            Equipment.YANG,
            Equipment.DIAMOND_STATIC_BRACELET,
            Equipment.LIGHTNING_FLASH
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo005(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 41);
            builder.allocate(SkillPoint.DEXTERITY, 63);
            builder.allocate(SkillPoint.INTELLIGENCE, 85);
            builder.equipment(
                Equipment.CAESURA,
                Equipment.TIME_RIFT,
                Equipment.ASPHYXIA,
                Equipment.STARDEW,
                Equipment.FINESSE,
                Equipment.FINESSE,
                Equipment.PROWESS,
                Equipment.AMANUENSIS
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.CAESURA,
            Equipment.TIME_RIFT,
            Equipment.ASPHYXIA,
            Equipment.STARDEW,
            Equipment.FINESSE,
            Equipment.FINESSE,
            Equipment.PROWESS,
            Equipment.AMANUENSIS
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo006(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 40);
            builder.allocate(SkillPoint.DEXTERITY, 65);
            builder.allocate(SkillPoint.INTELLIGENCE, 81);
            builder.equipment(
                Equipment.DWINDLED_KNOWLEDGE,
                Equipment.TIME_RIFT,
                Equipment.CHAOS_WOVEN_GREAVES,
                Equipment.STARDEW,
                Equipment.YANG,
                Equipment.YANG,
                Equipment.DIAMOND_HYDRO_BRACELET,
                Equipment.XEBEC
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.DWINDLED_KNOWLEDGE,
            Equipment.TIME_RIFT,
            Equipment.CHAOS_WOVEN_GREAVES,
            Equipment.STARDEW,
            Equipment.YANG,
            Equipment.YANG,
            Equipment.DIAMOND_HYDRO_BRACELET,
            Equipment.XEBEC
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo007(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.DEXTERITY, 61);
            builder.allocate(SkillPoint.INTELLIGENCE, 102);
            builder.equipment(
                Equipment.GNOSSIS,
                Equipment.TIME_RIFT,
                Equipment.ALEPH_NULL,
                Equipment.STARDEW,
                Equipment.YANG,
                Equipment.MOON_POOL_CIRCLET,
                Equipment.PROWESS,
                Equipment.DIAMOND_HYDRO_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.GNOSSIS,
            Equipment.TIME_RIFT,
            Equipment.ALEPH_NULL,
            Equipment.STARDEW,
            Equipment.YANG,
            Equipment.MOON_POOL_CIRCLET,
            Equipment.PROWESS,
            Equipment.DIAMOND_HYDRO_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo008(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 32);
            builder.allocate(SkillPoint.INTELLIGENCE, 76);
            builder.allocate(SkillPoint.DEFENCE, 72);
            builder.equipment(
                Equipment.AQUAMARINE,
                Equipment.TIME_RIFT,
                Equipment.VAWARD,
                Equipment.RESURGENCE,
                Equipment.OLD_KEEPERS_RING,
                Equipment.YANG,
                Equipment.VINDICATOR,
                Equipment.GIGABYTE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.AQUAMARINE,
            Equipment.TIME_RIFT,
            Equipment.VAWARD,
            Equipment.RESURGENCE,
            Equipment.OLD_KEEPERS_RING,
            Equipment.YANG,
            Equipment.VINDICATOR,
            Equipment.GIGABYTE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo009(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 56);
            builder.allocate(SkillPoint.DEFENCE, 55);
            builder.allocate(SkillPoint.AGILITY, 65);
            builder.equipment(
                Equipment.TITANOMACHIA,
                Equipment.FIREBIRD,
                Equipment.FIRE_SANCTUARY,
                Equipment.BOREAL,
                Equipment.DOWNFALL,
                Equipment.BYGG,
                Equipment.DUPLIBLAZE,
                Equipment.STROBELIGHT
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.TITANOMACHIA,
            Equipment.FIREBIRD,
            Equipment.FIRE_SANCTUARY,
            Equipment.BOREAL,
            Equipment.DOWNFALL,
            Equipment.BYGG,
            Equipment.DUPLIBLAZE,
            Equipment.STROBELIGHT
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo010(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 65);
            builder.allocate(SkillPoint.AGILITY, 61);
            builder.equipment(
                Equipment.DUNE_STORM,
                Equipment.CONDUIT_OF_SPIRIT,
                Equipment.SAGITTARIUS,
                Equipment.REVENANT,
                Equipment.DIAMOND_STEAM_RING,
                Equipment.INTENSITY,
                Equipment.VORTEX_BRACER,
                Equipment.NECKLACE_OF_A_THOUSAND_STORMS
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.DUNE_STORM,
            Equipment.CONDUIT_OF_SPIRIT,
            Equipment.SAGITTARIUS,
            Equipment.REVENANT,
            Equipment.DIAMOND_STEAM_RING,
            Equipment.INTENSITY,
            Equipment.VORTEX_BRACER,
            Equipment.NECKLACE_OF_A_THOUSAND_STORMS
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo011(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 85);
            builder.allocate(SkillPoint.DEXTERITY, 30);
            builder.allocate(SkillPoint.INTELLIGENCE, 18);
            builder.allocate(SkillPoint.AGILITY, 30);
            builder.equipment(
                Equipment.THE_SIRENS_CALL,
                Equipment.GALES_FREEDOM,
                Equipment.CHAIN_RULE,
                Equipment.EARTHSKY_ECLIPSE,
                Equipment.OLIVE,
                Equipment.OLIVE,
                Equipment.THANOS_BANNER,
                Equipment.RENDA_LANGIT
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.THE_SIRENS_CALL,
            Equipment.GALES_FREEDOM,
            Equipment.CHAIN_RULE,
            Equipment.EARTHSKY_ECLIPSE,
            Equipment.OLIVE,
            Equipment.OLIVE,
            Equipment.THANOS_BANNER,
            Equipment.RENDA_LANGIT
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo012(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 72);
            builder.allocate(SkillPoint.DEXTERITY, 74);
            builder.allocate(SkillPoint.AGILITY, 55);
            builder.equipment(
                Equipment.NEPHILIM,
                Equipment.TWILIGHT_GILDED_CLOAK,
                Equipment.PHYSALIS,
                Equipment.WARCHIEF,
                Equipment.BREEZEHANDS,
                Equipment.DIAMOND_FIBER_RING,
                Equipment.DIAMOND_FIBER_BRACELET,
                Equipment.DIAMOND_STATIC_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.NEPHILIM,
            Equipment.TWILIGHT_GILDED_CLOAK,
            Equipment.PHYSALIS,
            Equipment.WARCHIEF,
            Equipment.BREEZEHANDS,
            Equipment.DIAMOND_FIBER_RING,
            Equipment.DIAMOND_FIBER_BRACELET,
            Equipment.DIAMOND_STATIC_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo013(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 38);
            builder.allocate(SkillPoint.DEXTERITY, 76);
            builder.allocate(SkillPoint.DEFENCE, 50);
            builder.equipment(
                Equipment.BRAINWASH,
                Equipment.BETE_NOIRE,
                Equipment.DOOMSDAY_OMEN,
                Equipment.MANTLEWALKERS,
                Equipment.DOWNFALL,
                Equipment.DOWNFALL,
                Equipment.ENMITY,
                Equipment.CONTRAST
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.BRAINWASH,
            Equipment.BETE_NOIRE,
            Equipment.DOOMSDAY_OMEN,
            Equipment.MANTLEWALKERS,
            Equipment.DOWNFALL,
            Equipment.DOWNFALL,
            Equipment.ENMITY,
            Equipment.CONTRAST
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo014(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 70);
            builder.allocate(SkillPoint.DEXTERITY, 66);
            builder.allocate(SkillPoint.AGILITY, 58);
            builder.equipment(
                Equipment.LUMINIFEROUS_AETHER,
                Equipment.TWILIGHT_GILDED_CLOAK,
                Equipment.LEICTREACH_MAKANI,
                Equipment.WARCHIEF,
                Equipment.BREEZEHANDS,
                Equipment.DASHER,
                Equipment.RYCARS_BRAVADO,
                Equipment.DIAMOND_STATIC_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.LUMINIFEROUS_AETHER,
            Equipment.TWILIGHT_GILDED_CLOAK,
            Equipment.LEICTREACH_MAKANI,
            Equipment.WARCHIEF,
            Equipment.BREEZEHANDS,
            Equipment.DASHER,
            Equipment.RYCARS_BRAVADO,
            Equipment.DIAMOND_STATIC_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo015(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 20);
            builder.allocate(SkillPoint.DEXTERITY, 45);
            builder.allocate(SkillPoint.AGILITY, 80);
            builder.equipment(
                Equipment.UNRAVEL,
                Equipment.WANDERLUST,
                Equipment.SAGITTARIUS,
                Equipment.SKIDBLADNIR,
                Equipment.INTENSITY,
                Equipment.DIAMOND_STEAM_RING,
                Equipment.BUSTER_BRACER,
                Equipment.METAMORPHOSIS
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.UNRAVEL,
            Equipment.WANDERLUST,
            Equipment.SAGITTARIUS,
            Equipment.SKIDBLADNIR,
            Equipment.INTENSITY,
            Equipment.DIAMOND_STEAM_RING,
            Equipment.BUSTER_BRACER,
            Equipment.METAMORPHOSIS
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo016(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 70);
            builder.allocate(SkillPoint.INTELLIGENCE, 54);
            builder.allocate(SkillPoint.AGILITY, 80);
            builder.equipment(
                Equipment.APHOTIC,
                Equipment.CONDUIT_OF_SPIRIT,
                Equipment.ANAEROBIC,
                Equipment.MOONTOWER,
                Equipment.DRAOI_FAIR,
                Equipment.YANG,
                Equipment.ANYAS_PENUMBRA,
                Equipment.XEBEC
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.APHOTIC,
            Equipment.CONDUIT_OF_SPIRIT,
            Equipment.ANAEROBIC,
            Equipment.MOONTOWER,
            Equipment.DRAOI_FAIR,
            Equipment.YANG,
            Equipment.ANYAS_PENUMBRA,
            Equipment.XEBEC
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo017(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 60);
            builder.allocate(SkillPoint.INTELLIGENCE, 61);
            builder.allocate(SkillPoint.AGILITY, 80);
            builder.equipment(
                Equipment.APHOTIC,
                Equipment.TIME_RIFT,
                Equipment.TAO,
                Equipment.MOONTOWER,
                Equipment.YANG,
                Equipment.MOON_POOL_CIRCLET,
                Equipment.DIAMOND_HYDRO_BRACELET,
                Equipment.XEBEC
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.APHOTIC,
            Equipment.TIME_RIFT,
            Equipment.TAO,
            Equipment.MOONTOWER,
            Equipment.YANG,
            Equipment.MOON_POOL_CIRCLET,
            Equipment.DIAMOND_HYDRO_BRACELET,
            Equipment.XEBEC
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo018(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 49);
            builder.allocate(SkillPoint.INTELLIGENCE, 56);
            builder.allocate(SkillPoint.AGILITY, 80);
            builder.equipment(
                Equipment.APHOTIC,
                Equipment.LEVIATHAN,
                Equipment.TAO,
                Equipment.MOONTOWER,
                Equipment.YANG,
                Equipment.YANG,
                Equipment.DIAMOND_HYDRO_BRACELET,
                Equipment.DIAMOND_HYDRO_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.APHOTIC,
            Equipment.LEVIATHAN,
            Equipment.TAO,
            Equipment.MOONTOWER,
            Equipment.YANG,
            Equipment.YANG,
            Equipment.DIAMOND_HYDRO_BRACELET,
            Equipment.DIAMOND_HYDRO_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo019(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 51);
            builder.allocate(SkillPoint.DEXTERITY, 79);
            builder.allocate(SkillPoint.INTELLIGENCE, 39);
            builder.equipment(
                Equipment.NYCHTHEMERON,
                Equipment.DELIRIUM,
                Equipment.CHAOS_WOVEN_GREAVES,
                Equipment.ELECTRO_MAGES_BOOTS,
                Equipment.PHOTON,
                Equipment.DRAOI_FAIR,
                Equipment.DIAMOND_STATIC_BRACELET,
                Equipment.METAMORPHOSIS
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.NYCHTHEMERON,
            Equipment.DELIRIUM,
            Equipment.CHAOS_WOVEN_GREAVES,
            Equipment.ELECTRO_MAGES_BOOTS,
            Equipment.PHOTON,
            Equipment.DRAOI_FAIR,
            Equipment.DIAMOND_STATIC_BRACELET,
            Equipment.METAMORPHOSIS
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo020(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 61);
            builder.allocate(SkillPoint.DEXTERITY, 54);
            builder.allocate(SkillPoint.INTELLIGENCE, 26);
            builder.allocate(SkillPoint.DEFENCE, 46);
            builder.equipment(
                Equipment.NUCLEAR_EMESIS,
                Equipment.BETE_NOIRE,
                Equipment.TERA,
                Equipment.NETHERS_SCAR,
                Equipment.OLIVE,
                Equipment.OLIVE,
                Equipment.PROWESS,
                Equipment.SIMULACRUM
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.NUCLEAR_EMESIS,
            Equipment.BETE_NOIRE,
            Equipment.TERA,
            Equipment.NETHERS_SCAR,
            Equipment.OLIVE,
            Equipment.OLIVE,
            Equipment.PROWESS,
            Equipment.SIMULACRUM
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo021(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 60);
            builder.allocate(SkillPoint.DEFENCE, 79);
            builder.equipment(
                Equipment.NUCLEAR_EMESIS,
                Equipment.CANNONADE,
                Equipment.CHAIN_RULE,
                Equipment.CRUSADE_SABATONS,
                Equipment.DOWNFALL,
                Equipment.DOWNFALL,
                Equipment.DUPLIBLAZE,
                Equipment.SIMULACRUM
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.NUCLEAR_EMESIS,
            Equipment.CANNONADE,
            Equipment.CHAIN_RULE,
            Equipment.CRUSADE_SABATONS,
            Equipment.DOWNFALL,
            Equipment.DOWNFALL,
            Equipment.DUPLIBLAZE,
            Equipment.SIMULACRUM
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo022(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 30);
            builder.allocate(SkillPoint.DEXTERITY, 30);
            builder.allocate(SkillPoint.INTELLIGENCE, 65);
            builder.allocate(SkillPoint.DEFENCE, 30);
            builder.allocate(SkillPoint.AGILITY, 30);
            builder.equipment(
                Equipment.THIRD_EYE,
                Equipment.LIBRA,
                Equipment.RAINBOW_SANCTUARY,
                Equipment.MARTINGALE,
                Equipment.INTENSITY,
                Equipment.MOON_POOL_CIRCLET,
                Equipment.SUCCESSION,
                Equipment.DIAMOND_FUSION_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.THIRD_EYE,
            Equipment.LIBRA,
            Equipment.RAINBOW_SANCTUARY,
            Equipment.MARTINGALE,
            Equipment.INTENSITY,
            Equipment.MOON_POOL_CIRCLET,
            Equipment.SUCCESSION,
            Equipment.DIAMOND_FUSION_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo023(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 47);
            builder.allocate(SkillPoint.DEXTERITY, 75);
            builder.allocate(SkillPoint.DEFENCE, 70);
            builder.equipment(
                Equipment.BRAINWASH,
                Equipment.BETE_NOIRE,
                Equipment.ASPHYXIA,
                Equipment.CRUSADE_SABATONS,
                Equipment.INTENSITY,
                Equipment.DOWNFALL,
                Equipment.DIAMOND_STATIC_BRACELET,
                Equipment.SIMULACRUM
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.BRAINWASH,
            Equipment.BETE_NOIRE,
            Equipment.ASPHYXIA,
            Equipment.CRUSADE_SABATONS,
            Equipment.INTENSITY,
            Equipment.DOWNFALL,
            Equipment.DIAMOND_STATIC_BRACELET,
            Equipment.SIMULACRUM
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo024(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 41);
            builder.allocate(SkillPoint.DEXTERITY, 48);
            builder.allocate(SkillPoint.INTELLIGENCE, 37);
            builder.allocate(SkillPoint.AGILITY, 61);
            builder.equipment(
                Equipment.CAESURA,
                Equipment.ETIOLATION,
                Equipment.ALEPH_NULL,
                Equipment.VIRTUOSO,
                Equipment.INGRESS,
                Equipment.INGRESS,
                Equipment.PROWESS,
                Equipment.METAMORPHOSIS
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.CAESURA,
            Equipment.ETIOLATION,
            Equipment.ALEPH_NULL,
            Equipment.VIRTUOSO,
            Equipment.INGRESS,
            Equipment.INGRESS,
            Equipment.PROWESS,
            Equipment.METAMORPHOSIS
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo025(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 60);
            builder.allocate(SkillPoint.DEXTERITY, 32);
            builder.allocate(SkillPoint.INTELLIGENCE, 39);
            builder.allocate(SkillPoint.AGILITY, 70);
            builder.equipment(
                Equipment.DUNE_STORM,
                Equipment.ECHOES_OF_THE_LOST,
                Equipment.SAGITTARIUS,
                Equipment.REVENANT,
                Equipment.FINESSE,
                Equipment.FINESSE,
                Equipment.MELANCHOLIA,
                Equipment.RECKONING
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.DUNE_STORM,
            Equipment.ECHOES_OF_THE_LOST,
            Equipment.SAGITTARIUS,
            Equipment.REVENANT,
            Equipment.FINESSE,
            Equipment.FINESSE,
            Equipment.MELANCHOLIA,
            Equipment.RECKONING
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo026(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 47);
            builder.allocate(SkillPoint.DEXTERITY, 85);
            builder.allocate(SkillPoint.DEFENCE, 70);
            builder.equipment(
                Equipment.BRAINWASH,
                Equipment.INSIGNIA,
                Equipment.ASPHYXIA,
                Equipment.CRUSADE_SABATONS,
                Equipment.DIAMOND_STATIC_RING,
                Equipment.INTENSITY,
                Equipment.DIAMOND_STATIC_BRACELET,
                Equipment.SIMULACRUM
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.BRAINWASH,
            Equipment.INSIGNIA,
            Equipment.ASPHYXIA,
            Equipment.CRUSADE_SABATONS,
            Equipment.DIAMOND_STATIC_RING,
            Equipment.INTENSITY,
            Equipment.DIAMOND_STATIC_BRACELET,
            Equipment.SIMULACRUM
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo027(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 45);
            builder.allocate(SkillPoint.DEXTERITY, 84);
            builder.allocate(SkillPoint.AGILITY, 55);
            builder.equipment(
                Equipment.BRAINWASH,
                Equipment.ETIOLATION,
                Equipment.ORNATE_SHADOW_COVER,
                Equipment.WEATHERWALKERS,
                Equipment.GLOOMSTONE,
                Equipment.GLOOMSTONE,
                Equipment.DIAMOND_STATIC_BRACELET,
                Equipment.NECKLACE_OF_A_THOUSAND_STORMS
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.BRAINWASH,
            Equipment.ETIOLATION,
            Equipment.ORNATE_SHADOW_COVER,
            Equipment.WEATHERWALKERS,
            Equipment.GLOOMSTONE,
            Equipment.GLOOMSTONE,
            Equipment.DIAMOND_STATIC_BRACELET,
            Equipment.NECKLACE_OF_A_THOUSAND_STORMS
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo028(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 70);
            builder.allocate(SkillPoint.DEXTERITY, 70);
            builder.allocate(SkillPoint.INTELLIGENCE, 57);
            builder.equipment(
                Equipment.CAESURA,
                Equipment.BETE_NOIRE,
                Equipment.ASPHYXIA,
                Equipment.STARDEW,
                Equipment.MOON_POOL_CIRCLET,
                Equipment.COLD_WAVE,
                Equipment.DIAMOND_STATIC_BRACELET,
                Equipment.LIGHTNING_FLASH
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.CAESURA,
            Equipment.BETE_NOIRE,
            Equipment.ASPHYXIA,
            Equipment.STARDEW,
            Equipment.MOON_POOL_CIRCLET,
            Equipment.COLD_WAVE,
            Equipment.DIAMOND_STATIC_BRACELET,
            Equipment.LIGHTNING_FLASH
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo029(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 41);
            builder.allocate(SkillPoint.DEXTERITY, 38);
            builder.allocate(SkillPoint.INTELLIGENCE, 33);
            builder.allocate(SkillPoint.DEFENCE, 10);
            builder.allocate(SkillPoint.AGILITY, 52);
            builder.equipment(
                Equipment.CAESURA,
                Equipment.ETIOLATION,
                Equipment.ALEPH_NULL,
                Equipment.EARTHSKY_ECLIPSE,
                Equipment.INGRESS,
                Equipment.INGRESS,
                Equipment.PROWESS,
                Equipment.DIAMOND_FUSION_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.CAESURA,
            Equipment.ETIOLATION,
            Equipment.ALEPH_NULL,
            Equipment.EARTHSKY_ECLIPSE,
            Equipment.INGRESS,
            Equipment.INGRESS,
            Equipment.PROWESS,
            Equipment.DIAMOND_FUSION_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo030(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 39);
            builder.allocate(SkillPoint.INTELLIGENCE, 39);
            builder.allocate(SkillPoint.AGILITY, 99);
            builder.equipment(
                Equipment.AQUAMARINE,
                Equipment.CONDUIT_OF_SPIRIT,
                Equipment.WINDBORNE,
                Equipment.SKIDBLADNIR,
                Equipment.DIAMOND_STEAM_RING,
                Equipment.PHOTON,
                Equipment.VORTEX_BRACER,
                Equipment.CONTRAST
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.AQUAMARINE,
            Equipment.CONDUIT_OF_SPIRIT,
            Equipment.WINDBORNE,
            Equipment.SKIDBLADNIR,
            Equipment.DIAMOND_STEAM_RING,
            Equipment.PHOTON,
            Equipment.VORTEX_BRACER,
            Equipment.CONTRAST
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo031(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 48);
            builder.allocate(SkillPoint.INTELLIGENCE, 63);
            builder.allocate(SkillPoint.AGILITY, 77);
            builder.equipment(
                Equipment.AQUAMARINE,
                Equipment.DISCOVERER,
                Equipment.ELDER_OAK_ROOTS,
                Equipment.MOONTOWER,
                Equipment.OLD_KEEPERS_RING,
                Equipment.COLD_WAVE,
                Equipment.PROVENANCE,
                Equipment.CONTRAST
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.AQUAMARINE,
            Equipment.DISCOVERER,
            Equipment.ELDER_OAK_ROOTS,
            Equipment.MOONTOWER,
            Equipment.OLD_KEEPERS_RING,
            Equipment.COLD_WAVE,
            Equipment.PROVENANCE,
            Equipment.CONTRAST
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo032(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 60);
            builder.allocate(SkillPoint.INTELLIGENCE, 40);
            builder.allocate(SkillPoint.DEFENCE, 78);
            builder.equipment(
                Equipment.TITANOMACHIA,
                Equipment.CANNONADE,
                Equipment.CHAIN_RULE,
                Equipment.CRUSADE_SABATONS,
                Equipment.COLD_WAVE,
                Equipment.DOWNFALL,
                Equipment.ENMITY,
                Equipment.SIMULACRUM
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.TITANOMACHIA,
            Equipment.CANNONADE,
            Equipment.CHAIN_RULE,
            Equipment.CRUSADE_SABATONS,
            Equipment.COLD_WAVE,
            Equipment.DOWNFALL,
            Equipment.ENMITY,
            Equipment.SIMULACRUM
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo033(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 47);
            builder.allocate(SkillPoint.DEXTERITY, 70);
            builder.allocate(SkillPoint.DEFENCE, 81);
            builder.equipment(
                Equipment.BRAINWASH,
                Equipment.CANNONADE,
                Equipment.FLUMMOX,
                Equipment.CRUSADE_SABATONS,
                Equipment.INTENSITY,
                Equipment.DOWNFALL,
                Equipment.ENMITY,
                Equipment.SIMULACRUM
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.BRAINWASH,
            Equipment.CANNONADE,
            Equipment.FLUMMOX,
            Equipment.CRUSADE_SABATONS,
            Equipment.INTENSITY,
            Equipment.DOWNFALL,
            Equipment.ENMITY,
            Equipment.SIMULACRUM
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo034(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 72);
            builder.allocate(SkillPoint.DEXTERITY, 54);
            builder.allocate(SkillPoint.INTELLIGENCE, 30);
            builder.allocate(SkillPoint.AGILITY, 35);
            builder.equipment(
                Equipment.CAESURA,
                Equipment.DELIRIUM,
                Equipment.CHAIN_RULE,
                Equipment.EARTHSKY_ECLIPSE,
                Equipment.INTENSITY,
                Equipment.COLD_WAVE,
                Equipment.MELANCHOLIA,
                Equipment.BOTTLED_THUNDERSTORM
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.CAESURA,
            Equipment.DELIRIUM,
            Equipment.CHAIN_RULE,
            Equipment.EARTHSKY_ECLIPSE,
            Equipment.INTENSITY,
            Equipment.COLD_WAVE,
            Equipment.MELANCHOLIA,
            Equipment.BOTTLED_THUNDERSTORM
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo035(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.DEXTERITY, 46);
            builder.allocate(SkillPoint.INTELLIGENCE, 68);
            builder.allocate(SkillPoint.DEFENCE, 46);
            builder.equipment(
                Equipment.TRANSPLANTED_PSYCHE,
                Equipment.SOUL_SIGNAL,
                Equipment.ALEPH_NULL,
                Equipment.STARDEW,
                Equipment.YANG,
                Equipment.YANG,
                Equipment.PROWESS,
                Equipment.DIAMOND_HYDRO_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.TRANSPLANTED_PSYCHE,
            Equipment.SOUL_SIGNAL,
            Equipment.ALEPH_NULL,
            Equipment.STARDEW,
            Equipment.YANG,
            Equipment.YANG,
            Equipment.PROWESS,
            Equipment.DIAMOND_HYDRO_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo036(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.DEXTERITY, 43);
            builder.allocate(SkillPoint.INTELLIGENCE, 86);
            builder.allocate(SkillPoint.AGILITY, 34);
            builder.equipment(
                Equipment.CUMULONIMBUS,
                Equipment.ETIOLATION,
                Equipment.ALEPH_NULL,
                Equipment.STARDEW,
                Equipment.STRATUS,
                Equipment.STRATUS,
                Equipment.PROWESS,
                Equipment.DIAMOND_HYDRO_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.CUMULONIMBUS,
            Equipment.ETIOLATION,
            Equipment.ALEPH_NULL,
            Equipment.STARDEW,
            Equipment.STRATUS,
            Equipment.STRATUS,
            Equipment.PROWESS,
            Equipment.DIAMOND_HYDRO_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo037(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 41);
            builder.allocate(SkillPoint.DEXTERITY, 96);
            builder.allocate(SkillPoint.INTELLIGENCE, 36);
            builder.equipment(
                Equipment.CAESURA,
                Equipment.STRATOSPHERE,
                Equipment.ALEPH_NULL,
                Equipment.GALLEON,
                Equipment.OLIVE,
                Equipment.OLIVE,
                Equipment.PROWESS,
                Equipment.DIAMOND_STATIC_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.CAESURA,
            Equipment.STRATOSPHERE,
            Equipment.ALEPH_NULL,
            Equipment.GALLEON,
            Equipment.OLIVE,
            Equipment.OLIVE,
            Equipment.PROWESS,
            Equipment.DIAMOND_STATIC_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo038(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 43);
            builder.allocate(SkillPoint.DEXTERITY, 51);
            builder.allocate(SkillPoint.INTELLIGENCE, 27);
            builder.allocate(SkillPoint.DEFENCE, 60);
            builder.allocate(SkillPoint.AGILITY, 1);
            builder.equipment(
                Equipment.TRANSPLANTED_PSYCHE,
                Equipment.AZURITE,
                Equipment.ALEPH_NULL,
                Equipment.CRUSADE_SABATONS,
                Equipment.OLIVE,
                Equipment.OLIVE,
                Equipment.PROWESS,
                Equipment.UMAMI
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.TRANSPLANTED_PSYCHE,
            Equipment.AZURITE,
            Equipment.ALEPH_NULL,
            Equipment.CRUSADE_SABATONS,
            Equipment.OLIVE,
            Equipment.OLIVE,
            Equipment.PROWESS,
            Equipment.UMAMI
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo039(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 101);
            builder.allocate(SkillPoint.DEXTERITY, 31);
            builder.allocate(SkillPoint.INTELLIGENCE, 21);
            builder.allocate(SkillPoint.DEFENCE, 31);
            builder.allocate(SkillPoint.AGILITY, 46);
            builder.equipment(
                Equipment.GREATBIRD_EYRIE,
                Equipment.ETIOLATION,
                Equipment.PERCH_OF_THE_SHROUDED_SUN,
                Equipment.BOOTS_OF_BLUE_STONE,
                Equipment.OLIVE,
                Equipment.OLIVE,
                Equipment.PROWESS,
                Equipment.TENUTO
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.GREATBIRD_EYRIE,
            Equipment.ETIOLATION,
            Equipment.PERCH_OF_THE_SHROUDED_SUN,
            Equipment.BOOTS_OF_BLUE_STONE,
            Equipment.OLIVE,
            Equipment.OLIVE,
            Equipment.PROWESS,
            Equipment.TENUTO
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo040(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 47);
            builder.allocate(SkillPoint.DEXTERITY, 70);
            builder.allocate(SkillPoint.DEFENCE, 79);
            builder.equipment(
                Equipment.BRAINWASH,
                Equipment.CANNONADE,
                Equipment.FIRE_SANCTUARY,
                Equipment.CRUSADE_SABATONS,
                Equipment.INTENSITY,
                Equipment.DOWNFALL,
                Equipment.DUPLIBLAZE,
                Equipment.SIMULACRUM
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.BRAINWASH,
            Equipment.CANNONADE,
            Equipment.FIRE_SANCTUARY,
            Equipment.CRUSADE_SABATONS,
            Equipment.INTENSITY,
            Equipment.DOWNFALL,
            Equipment.DUPLIBLAZE,
            Equipment.SIMULACRUM
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo041(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 71);
            builder.allocate(SkillPoint.DEXTERITY, 73);
            builder.allocate(SkillPoint.AGILITY, 40);
            builder.equipment(
                Equipment.LUMINIFEROUS_AETHER,
                Equipment.TWILIGHT_GILDED_CLOAK,
                Equipment.POST_ULTIMA,
                Equipment.WARCHIEF,
                Equipment.DIAMOND_FIBER_RING,
                Equipment.DIAMOND_FIBER_RING,
                Equipment.DIAMOND_FIBER_BRACELET,
                Equipment.DIAMOND_STATIC_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.LUMINIFEROUS_AETHER,
            Equipment.TWILIGHT_GILDED_CLOAK,
            Equipment.POST_ULTIMA,
            Equipment.WARCHIEF,
            Equipment.DIAMOND_FIBER_RING,
            Equipment.DIAMOND_FIBER_RING,
            Equipment.DIAMOND_FIBER_BRACELET,
            Equipment.DIAMOND_STATIC_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo042(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.DEXTERITY, 64);
            builder.allocate(SkillPoint.INTELLIGENCE, 88);
            builder.allocate(SkillPoint.DEFENCE, 36);
            builder.equipment(
                Equipment.ANAMNESIS,
                Equipment.SCHADENFREUDE,
                Equipment.ALEPH_NULL,
                Equipment.REPURPOSED_VESSELS,
                Equipment.YANG,
                Equipment.YANG,
                Equipment.PROWESS,
                Equipment.DIAMOND_HYDRO_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.ANAMNESIS,
            Equipment.SCHADENFREUDE,
            Equipment.ALEPH_NULL,
            Equipment.REPURPOSED_VESSELS,
            Equipment.YANG,
            Equipment.YANG,
            Equipment.PROWESS,
            Equipment.DIAMOND_HYDRO_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo043(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 35);
            builder.allocate(SkillPoint.DEXTERITY, 65);
            builder.allocate(SkillPoint.INTELLIGENCE, 92);
            builder.equipment(
                Equipment.ANAMNESIS,
                Equipment.TIME_RIFT,
                Equipment.CHAOS_WOVEN_GREAVES,
                Equipment.STARDEW,
                Equipment.MOON_POOL_CIRCLET,
                Equipment.OLIVE,
                Equipment.BUSTER_BRACER,
                Equipment.DIAMOND_HYDRO_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.ANAMNESIS,
            Equipment.TIME_RIFT,
            Equipment.CHAOS_WOVEN_GREAVES,
            Equipment.STARDEW,
            Equipment.MOON_POOL_CIRCLET,
            Equipment.OLIVE,
            Equipment.BUSTER_BRACER,
            Equipment.DIAMOND_HYDRO_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo044(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 70);
            builder.allocate(SkillPoint.DEFENCE, 27);
            builder.allocate(SkillPoint.AGILITY, 60);
            builder.equipment(
                Equipment.GALES_SIGHT,
                Equipment.DISCOVERER,
                Equipment.PAIN_CYCLE,
                Equipment.REVENANT,
                Equipment.INGRESS,
                Equipment.BLOODBORNE,
                Equipment.VORTEX_BRACER,
                Equipment.CONTRAST
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.GALES_SIGHT,
            Equipment.DISCOVERER,
            Equipment.PAIN_CYCLE,
            Equipment.REVENANT,
            Equipment.INGRESS,
            Equipment.BLOODBORNE,
            Equipment.VORTEX_BRACER,
            Equipment.CONTRAST
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo045(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 57);
            builder.allocate(SkillPoint.DEXTERITY, 59);
            builder.allocate(SkillPoint.AGILITY, 60);
            builder.equipment(
                Equipment.BRAINWASH,
                Equipment.SOARFAE,
                Equipment.SAGITTARIUS,
                Equipment.REVENANT,
                Equipment.INGRESS,
                Equipment.INGRESS,
                Equipment.VORTEX_BRACER,
                Equipment.BOTTLED_THUNDERSTORM
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.BRAINWASH,
            Equipment.SOARFAE,
            Equipment.SAGITTARIUS,
            Equipment.REVENANT,
            Equipment.INGRESS,
            Equipment.INGRESS,
            Equipment.VORTEX_BRACER,
            Equipment.BOTTLED_THUNDERSTORM
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo046(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 62);
            builder.allocate(SkillPoint.DEXTERITY, 70);
            builder.allocate(SkillPoint.AGILITY, 63);
            builder.equipment(
                Equipment.BRAINWASH,
                Equipment.SOARFAE,
                Equipment.PAIN_CYCLE,
                Equipment.REVENANT,
                Equipment.INTENSITY,
                Equipment.DIAMOND_STEAM_RING,
                Equipment.VORTEX_BRACER,
                Equipment.NECKLACE_OF_A_THOUSAND_STORMS
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.BRAINWASH,
            Equipment.SOARFAE,
            Equipment.PAIN_CYCLE,
            Equipment.REVENANT,
            Equipment.INTENSITY,
            Equipment.DIAMOND_STEAM_RING,
            Equipment.VORTEX_BRACER,
            Equipment.NECKLACE_OF_A_THOUSAND_STORMS
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo047(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 55);
            builder.allocate(SkillPoint.DEFENCE, 27);
            builder.allocate(SkillPoint.AGILITY, 77);
            builder.equipment(
                Equipment.UNRAVEL,
                Equipment.DISCOVERER,
                Equipment.SAGITTARIUS,
                Equipment.SKIDBLADNIR,
                Equipment.INGRESS,
                Equipment.BLOODBORNE,
                Equipment.DOUBLE_VISION,
                Equipment.CONTRAST
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.UNRAVEL,
            Equipment.DISCOVERER,
            Equipment.SAGITTARIUS,
            Equipment.SKIDBLADNIR,
            Equipment.INGRESS,
            Equipment.BLOODBORNE,
            Equipment.DOUBLE_VISION,
            Equipment.CONTRAST
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo048(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 66);
            builder.allocate(SkillPoint.DEXTERITY, 72);
            builder.allocate(SkillPoint.AGILITY, 55);
            builder.equipment(
                Equipment.SCARLET_VEIL,
                Equipment.TWILIGHT_GILDED_CLOAK,
                Equipment.PHYSALIS,
                Equipment.WARCHIEF,
                Equipment.BREEZEHANDS,
                Equipment.DIAMOND_FIBER_RING,
                Equipment.DIAMOND_FIBER_BRACELET,
                Equipment.RECALCITRANCE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.SCARLET_VEIL,
            Equipment.TWILIGHT_GILDED_CLOAK,
            Equipment.PHYSALIS,
            Equipment.WARCHIEF,
            Equipment.BREEZEHANDS,
            Equipment.DIAMOND_FIBER_RING,
            Equipment.DIAMOND_FIBER_BRACELET,
            Equipment.RECALCITRANCE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo049(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 36);
            builder.allocate(SkillPoint.DEXTERITY, 41);
            builder.allocate(SkillPoint.INTELLIGENCE, 58);
            builder.allocate(SkillPoint.AGILITY, 31);
            builder.equipment(
                Equipment.GNOSSIS,
                Equipment.PHANTASMAGORIA,
                Equipment.ALEPH_NULL,
                Equipment.PRO_TEMPORE,
                Equipment.DRAOI_FAIR,
                Equipment.MOON_POOL_CIRCLET,
                Equipment.PROWESS,
                Equipment.CHARM_OF_THE_STORMS
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.GNOSSIS,
            Equipment.PHANTASMAGORIA,
            Equipment.ALEPH_NULL,
            Equipment.PRO_TEMPORE,
            Equipment.DRAOI_FAIR,
            Equipment.MOON_POOL_CIRCLET,
            Equipment.PROWESS,
            Equipment.CHARM_OF_THE_STORMS
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo050(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 32);
            builder.allocate(SkillPoint.DEXTERITY, 61);
            builder.allocate(SkillPoint.INTELLIGENCE, 62);
            builder.equipment(
                Equipment.PROSENCEPHALON,
                Equipment.UMBRAL_MAIL,
                Equipment.CHAOS_WOVEN_GREAVES,
                Equipment.STARDEW,
                Equipment.YANG,
                Equipment.PHOTON,
                Equipment.MISALIGNMENT,
                Equipment.METAMORPHOSIS
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.PROSENCEPHALON,
            Equipment.UMBRAL_MAIL,
            Equipment.CHAOS_WOVEN_GREAVES,
            Equipment.STARDEW,
            Equipment.YANG,
            Equipment.PHOTON,
            Equipment.MISALIGNMENT,
            Equipment.METAMORPHOSIS
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo051(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 39);
            builder.allocate(SkillPoint.DEXTERITY, 59);
            builder.allocate(SkillPoint.INTELLIGENCE, 59);
            builder.allocate(SkillPoint.DEFENCE, 39);
            builder.equipment(
                Equipment.PROSENCEPHALON,
                Equipment.STRATOSPHERE,
                Equipment.ENTANGLEMENT,
                Equipment.STARDEW,
                Equipment.OLIVE,
                Equipment.PHOTON,
                Equipment.DRAGONS_EYE_BRACELET,
                Equipment.CONTRAST
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.PROSENCEPHALON,
            Equipment.STRATOSPHERE,
            Equipment.ENTANGLEMENT,
            Equipment.STARDEW,
            Equipment.OLIVE,
            Equipment.PHOTON,
            Equipment.DRAGONS_EYE_BRACELET,
            Equipment.CONTRAST
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo052(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 63);
            builder.allocate(SkillPoint.DEXTERITY, 58);
            builder.allocate(SkillPoint.INTELLIGENCE, 30);
            builder.equipment(
                Equipment.NYCHTHEMERON,
                Equipment.TAURUS,
                Equipment.POST_ULTIMA,
                Equipment.BLIND_THRUST,
                Equipment.AD_TERRAM,
                Equipment.AD_TERRAM,
                Equipment.BLISSFUL_SOLACE,
                Equipment.H_209_MINIATURE_DEFIBRILLATOR
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.NYCHTHEMERON,
            Equipment.TAURUS,
            Equipment.POST_ULTIMA,
            Equipment.BLIND_THRUST,
            Equipment.AD_TERRAM,
            Equipment.AD_TERRAM,
            Equipment.BLISSFUL_SOLACE,
            Equipment.H_209_MINIATURE_DEFIBRILLATOR
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo053(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 60);
            builder.allocate(SkillPoint.INTELLIGENCE, 60);
            builder.allocate(SkillPoint.DEFENCE, 84);
            builder.equipment(
                Equipment.DREADNOUGHT,
                Equipment.CANNONADE,
                Equipment.OPHIUCHUS,
                Equipment.CRUSADE_SABATONS,
                Equipment.DRAOI_FAIR,
                Equipment.INTENSITY,
                Equipment.DUPLIBLAZE,
                Equipment.SIMULACRUM
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.DREADNOUGHT,
            Equipment.CANNONADE,
            Equipment.OPHIUCHUS,
            Equipment.CRUSADE_SABATONS,
            Equipment.DRAOI_FAIR,
            Equipment.INTENSITY,
            Equipment.DUPLIBLAZE,
            Equipment.SIMULACRUM
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo054(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 10);
            builder.allocate(SkillPoint.INTELLIGENCE, 60);
            builder.allocate(SkillPoint.DEFENCE, 104);
            builder.equipment(
                Equipment.DREADNOUGHT,
                Equipment.CANNONADE,
                Equipment.OPHIUCHUS,
                Equipment.MANTLEWALKERS,
                Equipment.HELLION,
                Equipment.DRAOI_FAIR,
                Equipment.DIAMOND_SOLAR_BRACELET,
                Equipment.ABRASION
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.DREADNOUGHT,
            Equipment.CANNONADE,
            Equipment.OPHIUCHUS,
            Equipment.MANTLEWALKERS,
            Equipment.HELLION,
            Equipment.DRAOI_FAIR,
            Equipment.DIAMOND_SOLAR_BRACELET,
            Equipment.ABRASION
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo055(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 56);
            builder.allocate(SkillPoint.DEXTERITY, 57);
            builder.allocate(SkillPoint.DEFENCE, 47);
            builder.equipment(
                Equipment.DARKSTEEL_FULL_HELM,
                Equipment.TAURUS,
                Equipment.EARTH_BREAKER,
                Equipment.DAWNBREAK,
                Equipment.DOWNFALL,
                Equipment.DOWNFALL,
                Equipment.MOMENTUM,
                Equipment.H_209_MINIATURE_DEFIBRILLATOR
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.DARKSTEEL_FULL_HELM,
            Equipment.TAURUS,
            Equipment.EARTH_BREAKER,
            Equipment.DAWNBREAK,
            Equipment.DOWNFALL,
            Equipment.DOWNFALL,
            Equipment.MOMENTUM,
            Equipment.H_209_MINIATURE_DEFIBRILLATOR
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo056(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 45);
            builder.allocate(SkillPoint.DEXTERITY, 62);
            builder.allocate(SkillPoint.INTELLIGENCE, 82);
            builder.equipment(
                Equipment.CAESURA,
                Equipment.AQUARIUS,
                Equipment.CHAOS_WOVEN_GREAVES,
                Equipment.STARDEW,
                Equipment.AZEOTROPE,
                Equipment.AZEOTROPE,
                Equipment.MISALIGNMENT,
                Equipment.DIAMOND_HYDRO_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.CAESURA,
            Equipment.AQUARIUS,
            Equipment.CHAOS_WOVEN_GREAVES,
            Equipment.STARDEW,
            Equipment.AZEOTROPE,
            Equipment.AZEOTROPE,
            Equipment.MISALIGNMENT,
            Equipment.DIAMOND_HYDRO_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo057(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 37);
            builder.allocate(SkillPoint.DEXTERITY, 62);
            builder.allocate(SkillPoint.INTELLIGENCE, 92);
            builder.equipment(
                Equipment.RESOLUTION,
                Equipment.AQUARIUS,
                Equipment.CHAOS_WOVEN_GREAVES,
                Equipment.STARDEW,
                Equipment.YANG,
                Equipment.YANG,
                Equipment.DIAMOND_HYDRO_BRACELET,
                Equipment.DIAMOND_HYDRO_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.RESOLUTION,
            Equipment.AQUARIUS,
            Equipment.CHAOS_WOVEN_GREAVES,
            Equipment.STARDEW,
            Equipment.YANG,
            Equipment.YANG,
            Equipment.DIAMOND_HYDRO_BRACELET,
            Equipment.DIAMOND_HYDRO_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo058(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 37);
            builder.allocate(SkillPoint.DEXTERITY, 52);
            builder.allocate(SkillPoint.INTELLIGENCE, 86);
            builder.equipment(
                Equipment.RESOLUTION,
                Equipment.SOUL_SIGNAL,
                Equipment.CHAOS_WOVEN_GREAVES,
                Equipment.STARDEW,
                Equipment.YANG,
                Equipment.YANG,
                Equipment.DIAMOND_HYDRO_BRACELET,
                Equipment.AUXETIC_CAPACITOR
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.RESOLUTION,
            Equipment.SOUL_SIGNAL,
            Equipment.CHAOS_WOVEN_GREAVES,
            Equipment.STARDEW,
            Equipment.YANG,
            Equipment.YANG,
            Equipment.DIAMOND_HYDRO_BRACELET,
            Equipment.AUXETIC_CAPACITOR
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo059(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 14);
            builder.allocate(SkillPoint.DEXTERITY, 12);
            builder.allocate(SkillPoint.INTELLIGENCE, 52);
            builder.allocate(SkillPoint.DEFENCE, 12);
            builder.allocate(SkillPoint.AGILITY, 51);
            builder.equipment(
                Equipment.SPECTRUM,
                Equipment.DISCOVERER,
                Equipment.VAWARD,
                Equipment.CAPRICORN,
                Equipment.SUMMA,
                Equipment.OLD_KEEPERS_RING,
                Equipment.VINDICATOR,
                Equipment.DIAMOND_FUSION_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.SPECTRUM,
            Equipment.DISCOVERER,
            Equipment.VAWARD,
            Equipment.CAPRICORN,
            Equipment.SUMMA,
            Equipment.OLD_KEEPERS_RING,
            Equipment.VINDICATOR,
            Equipment.DIAMOND_FUSION_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo060(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 40);
            builder.allocate(SkillPoint.DEXTERITY, 93);
            builder.allocate(SkillPoint.INTELLIGENCE, 55);
            builder.equipment(
                Equipment.NEURON,
                Equipment.STRATOSPHERE,
                Equipment.CHAOS_WOVEN_GREAVES,
                Equipment.PRO_TEMPORE,
                Equipment.DIAMOND_STATIC_RING,
                Equipment.LODESTONE,
                Equipment.DIAMOND_STATIC_BRACELET,
                Equipment.ABRASION
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.NEURON,
            Equipment.STRATOSPHERE,
            Equipment.CHAOS_WOVEN_GREAVES,
            Equipment.PRO_TEMPORE,
            Equipment.DIAMOND_STATIC_RING,
            Equipment.LODESTONE,
            Equipment.DIAMOND_STATIC_BRACELET,
            Equipment.ABRASION
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo061(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 83);
            builder.allocate(SkillPoint.DEXTERITY, 57);
            builder.allocate(SkillPoint.INTELLIGENCE, 57);
            builder.equipment(
                Equipment.CAESURA,
                Equipment.LEVIATHAN,
                Equipment.CHAIN_RULE,
                Equipment.CONDENSATION,
                Equipment.AZEOTROPE,
                Equipment.AZEOTROPE,
                Equipment.MISALIGNMENT,
                Equipment.XEBEC
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.CAESURA,
            Equipment.LEVIATHAN,
            Equipment.CHAIN_RULE,
            Equipment.CONDENSATION,
            Equipment.AZEOTROPE,
            Equipment.AZEOTROPE,
            Equipment.MISALIGNMENT,
            Equipment.XEBEC
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo062(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 46);
            builder.allocate(SkillPoint.DEXTERITY, 42);
            builder.allocate(SkillPoint.INTELLIGENCE, 45);
            builder.allocate(SkillPoint.AGILITY, 63);
            builder.equipment(
                Equipment.UNRAVEL,
                Equipment.PHANTASMAGORIA,
                Equipment.SAGITTARIUS,
                Equipment.STEAMJET_WALKERS,
                Equipment.SUMMA,
                Equipment.SUMMA,
                Equipment.BREAKTHROUGH,
                Equipment.EYES_ON_ALL
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.UNRAVEL,
            Equipment.PHANTASMAGORIA,
            Equipment.SAGITTARIUS,
            Equipment.STEAMJET_WALKERS,
            Equipment.SUMMA,
            Equipment.SUMMA,
            Equipment.BREAKTHROUGH,
            Equipment.EYES_ON_ALL
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo063(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 48);
            builder.allocate(SkillPoint.DEXTERITY, 31);
            builder.allocate(SkillPoint.INTELLIGENCE, 47);
            builder.allocate(SkillPoint.AGILITY, 35);
            builder.equipment(
                Equipment.CUMULONIMBUS,
                Equipment.PHANTASMAGORIA,
                Equipment.APOPHENIA,
                Equipment.STEAMJET_WALKERS,
                Equipment.INTENSITY,
                Equipment.FINESSE,
                Equipment.BREAKTHROUGH,
                Equipment.BOTTLED_THUNDERSTORM
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.CUMULONIMBUS,
            Equipment.PHANTASMAGORIA,
            Equipment.APOPHENIA,
            Equipment.STEAMJET_WALKERS,
            Equipment.INTENSITY,
            Equipment.FINESSE,
            Equipment.BREAKTHROUGH,
            Equipment.BOTTLED_THUNDERSTORM
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo064(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 29);
            builder.allocate(SkillPoint.DEXTERITY, 50);
            builder.allocate(SkillPoint.INTELLIGENCE, 68);
            builder.allocate(SkillPoint.DEFENCE, 27);
            builder.allocate(SkillPoint.AGILITY, 29);
            builder.equipment(
                Equipment.SPLINTERED_DAWN,
                Equipment.LIBRA,
                Equipment.RAINBOW_SANCTUARY,
                Equipment.STARDEW,
                Equipment.DRAOI_FAIR,
                Equipment.SUMMA,
                Equipment.PROWESS,
                Equipment.XEBEC
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.SPLINTERED_DAWN,
            Equipment.LIBRA,
            Equipment.RAINBOW_SANCTUARY,
            Equipment.STARDEW,
            Equipment.DRAOI_FAIR,
            Equipment.SUMMA,
            Equipment.PROWESS,
            Equipment.XEBEC
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo065(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 25);
            builder.allocate(SkillPoint.DEXTERITY, 25);
            builder.allocate(SkillPoint.INTELLIGENCE, 65);
            builder.allocate(SkillPoint.DEFENCE, 25);
            builder.allocate(SkillPoint.AGILITY, 25);
            builder.equipment(
                Equipment.SPECTRUM,
                Equipment.LIBRA,
                Equipment.RAINBOW_SANCTUARY,
                Equipment.MARTINGALE,
                Equipment.MOON_POOL_CIRCLET,
                Equipment.PHOTON,
                Equipment.SUCCESSION,
                Equipment.DIAMOND_FUSION_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.SPECTRUM,
            Equipment.LIBRA,
            Equipment.RAINBOW_SANCTUARY,
            Equipment.MARTINGALE,
            Equipment.MOON_POOL_CIRCLET,
            Equipment.PHOTON,
            Equipment.SUCCESSION,
            Equipment.DIAMOND_FUSION_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo066(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 29);
            builder.allocate(SkillPoint.DEXTERITY, 50);
            builder.allocate(SkillPoint.INTELLIGENCE, 68);
            builder.allocate(SkillPoint.DEFENCE, 27);
            builder.allocate(SkillPoint.AGILITY, 29);
            builder.equipment(
                Equipment.SPLINTERED_DAWN,
                Equipment.LIBRA,
                Equipment.RAINBOW_SANCTUARY,
                Equipment.STARDEW,
                Equipment.DRAOI_FAIR,
                Equipment.SUMMA,
                Equipment.PROWESS,
                Equipment.XEBEC
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.SPLINTERED_DAWN,
            Equipment.LIBRA,
            Equipment.RAINBOW_SANCTUARY,
            Equipment.STARDEW,
            Equipment.DRAOI_FAIR,
            Equipment.SUMMA,
            Equipment.PROWESS,
            Equipment.XEBEC
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo067(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 29);
            builder.allocate(SkillPoint.DEXTERITY, 50);
            builder.allocate(SkillPoint.INTELLIGENCE, 63);
            builder.allocate(SkillPoint.DEFENCE, 27);
            builder.allocate(SkillPoint.AGILITY, 29);
            builder.equipment(
                Equipment.SPLINTERED_DAWN,
                Equipment.LIBRA,
                Equipment.RAINBOW_SANCTUARY,
                Equipment.STARDEW,
                Equipment.AZEOTROPE,
                Equipment.SUMMA,
                Equipment.DRAGONS_EYE_BRACELET,
                Equipment.DIAMOND_FUSION_NECKLACE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.SPLINTERED_DAWN,
            Equipment.LIBRA,
            Equipment.RAINBOW_SANCTUARY,
            Equipment.STARDEW,
            Equipment.AZEOTROPE,
            Equipment.SUMMA,
            Equipment.DRAGONS_EYE_BRACELET,
            Equipment.DIAMOND_FUSION_NECKLACE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo068(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 40);
            builder.allocate(SkillPoint.DEXTERITY, 50);
            builder.allocate(SkillPoint.INTELLIGENCE, 50);
            builder.allocate(SkillPoint.DEFENCE, 36);
            builder.equipment(
                Equipment.GUILLOTINE,
                Equipment.SOUL_SIGNAL,
                Equipment.ALEPH_NULL,
                Equipment.FRENZIED_MOCKERY,
                Equipment.AZEOTROPE,
                Equipment.AZEOTROPE,
                Equipment.VENERATION,
                Equipment.TENUTO
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.GUILLOTINE,
            Equipment.SOUL_SIGNAL,
            Equipment.ALEPH_NULL,
            Equipment.FRENZIED_MOCKERY,
            Equipment.AZEOTROPE,
            Equipment.AZEOTROPE,
            Equipment.VENERATION,
            Equipment.TENUTO
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo069(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 77);
            builder.allocate(SkillPoint.DEXTERITY, 67);
            builder.allocate(SkillPoint.DEFENCE, 40);
            builder.equipment(
                Equipment.OBSIDIAN_FRAMED_HELMET,
                Equipment.TAURUS,
                Equipment.WRITHING_GROWTH,
                Equipment.BLIND_THRUST,
                Equipment.AD_TERRAM,
                Equipment.AD_TERRAM,
                Equipment.MOMENTUM,
                Equipment.H_209_MINIATURE_DEFIBRILLATOR
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.OBSIDIAN_FRAMED_HELMET,
            Equipment.TAURUS,
            Equipment.WRITHING_GROWTH,
            Equipment.BLIND_THRUST,
            Equipment.AD_TERRAM,
            Equipment.AD_TERRAM,
            Equipment.MOMENTUM,
            Equipment.H_209_MINIATURE_DEFIBRILLATOR
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo070(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 61);
            builder.allocate(SkillPoint.DEXTERITY, 54);
            builder.allocate(SkillPoint.INTELLIGENCE, 26);
            builder.allocate(SkillPoint.DEFENCE, 46);
            builder.equipment(
                Equipment.NUCLEAR_EMESIS,
                Equipment.BETE_NOIRE,
                Equipment.TERA,
                Equipment.NETHERS_SCAR,
                Equipment.OLIVE,
                Equipment.SUPPRESSION,
                Equipment.PROWESS,
                Equipment.SIMULACRUM
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.NUCLEAR_EMESIS,
            Equipment.BETE_NOIRE,
            Equipment.TERA,
            Equipment.NETHERS_SCAR,
            Equipment.OLIVE,
            Equipment.SUPPRESSION,
            Equipment.PROWESS,
            Equipment.SIMULACRUM
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo071(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 45);
            builder.allocate(SkillPoint.DEXTERITY, 50);
            builder.allocate(SkillPoint.DEFENCE, 41);
            builder.allocate(SkillPoint.AGILITY, 49);
            builder.equipment(
                Equipment.KINDLED_ORCHID,
                Equipment.CALIDADE_MAIL,
                Equipment.RUNEBOUND_CHAINS,
                Equipment.NETHERS_SCAR,
                Equipment.FLASHFIRE_KNUCKLE,
                Equipment.BREEZEHANDS,
                Equipment.FLASHFIRE_GAUNTLET,
                Equipment.RECALCITRANCE
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.KINDLED_ORCHID,
            Equipment.CALIDADE_MAIL,
            Equipment.RUNEBOUND_CHAINS,
            Equipment.NETHERS_SCAR,
            Equipment.FLASHFIRE_KNUCKLE,
            Equipment.BREEZEHANDS,
            Equipment.FLASHFIRE_GAUNTLET,
            Equipment.RECALCITRANCE
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo072(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 75);
            builder.allocate(SkillPoint.DEXTERITY, 57);
            builder.allocate(SkillPoint.DEFENCE, 45);
            builder.equipment(
                Equipment.DARKSTEEL_FULL_HELM,
                Equipment.TAURUS,
                Equipment.WRITHING_GROWTH,
                Equipment.BLIND_THRUST,
                Equipment.LOST_SECONDS,
                Equipment.LOST_SECONDS,
                Equipment.MOMENTUM,
                Equipment.H_209_MINIATURE_DEFIBRILLATOR
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.DARKSTEEL_FULL_HELM,
            Equipment.TAURUS,
            Equipment.WRITHING_GROWTH,
            Equipment.BLIND_THRUST,
            Equipment.LOST_SECONDS,
            Equipment.LOST_SECONDS,
            Equipment.MOMENTUM,
            Equipment.H_209_MINIATURE_DEFIBRILLATOR
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo073(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 58);
            builder.allocate(SkillPoint.AGILITY, 74);
            builder.equipment(
                Equipment.DUNE_STORM,
                Equipment.CONDUIT_OF_SPIRIT,
                Equipment.SAGITTARIUS,
                Equipment.SKIDBLADNIR,
                Equipment.PHOTON,
                Equipment.PHOTON,
                Equipment.VORTEX_BRACER,
                Equipment.RENDA_LANGIT
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.DUNE_STORM,
            Equipment.CONDUIT_OF_SPIRIT,
            Equipment.SAGITTARIUS,
            Equipment.SKIDBLADNIR,
            Equipment.PHOTON,
            Equipment.PHOTON,
            Equipment.VORTEX_BRACER,
            Equipment.RENDA_LANGIT
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo074(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 30);
            builder.allocate(SkillPoint.DEXTERITY, 30);
            builder.allocate(SkillPoint.INTELLIGENCE, 44);
            builder.allocate(SkillPoint.AGILITY, 58);
            builder.equipment(
                Equipment.FILTER_MASK,
                Equipment.GALES_FREEDOM,
                Equipment.ALEPH_NULL,
                Equipment.SKIDBLADNIR,
                Equipment.YANG,
                Equipment.INTENSITY,
                Equipment.SYNAPSE,
                Equipment.RENDA_LANGIT
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.FILTER_MASK,
            Equipment.GALES_FREEDOM,
            Equipment.ALEPH_NULL,
            Equipment.SKIDBLADNIR,
            Equipment.YANG,
            Equipment.INTENSITY,
            Equipment.SYNAPSE,
            Equipment.RENDA_LANGIT
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo075(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 41);
            builder.allocate(SkillPoint.DEXTERITY, 84);
            builder.allocate(SkillPoint.DEFENCE, 64);
            builder.equipment(
                Equipment.BRAINWASH,
                Equipment.INSIGNIA,
                Equipment.ASPHYXIA,
                Equipment.ORNATE_SHADOW_CLOUD,
                Equipment.INTENSITY,
                Equipment.PHOTON,
                Equipment.DIAMOND_STATIC_BRACELET,
                Equipment.SIMULACRUM
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.BRAINWASH,
            Equipment.INSIGNIA,
            Equipment.ASPHYXIA,
            Equipment.ORNATE_SHADOW_CLOUD,
            Equipment.INTENSITY,
            Equipment.PHOTON,
            Equipment.DIAMOND_STATIC_BRACELET,
            Equipment.SIMULACRUM
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo076(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 40);
            builder.allocate(SkillPoint.DEXTERITY, 54);
            builder.allocate(SkillPoint.DEFENCE, 60);
            builder.allocate(SkillPoint.AGILITY, 40);
            builder.equipment(
                Equipment.OSSUARY,
                Equipment.FUTURE_SHOCK_PLATING,
                Equipment.RUNEBOUND_CHAINS,
                Equipment.THUNDEROUS_STEP,
                Equipment.AGAVE,
                Equipment.MICROCHIP,
                Equipment.MELANCHOLIA,
                Equipment.PULSE_STOPPER
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.OSSUARY,
            Equipment.FUTURE_SHOCK_PLATING,
            Equipment.RUNEBOUND_CHAINS,
            Equipment.THUNDEROUS_STEP,
            Equipment.AGAVE,
            Equipment.MICROCHIP,
            Equipment.MELANCHOLIA,
            Equipment.PULSE_STOPPER
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo077(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.AGILITY, 80);
            builder.equipment(
                Equipment.OSSUARY,
                Equipment.ECHOES_OF_THE_LOST,
                Equipment.ORNATE_SHADOW_COVER,
                Equipment.SKIDBLADNIR,
                Equipment.RASK,
                Equipment.RASK,
                Equipment.VORTEX_BRACER,
                Equipment.CONTRAST
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.OSSUARY,
            Equipment.ECHOES_OF_THE_LOST,
            Equipment.ORNATE_SHADOW_COVER,
            Equipment.SKIDBLADNIR,
            Equipment.RASK,
            Equipment.RASK,
            Equipment.VORTEX_BRACER,
            Equipment.CONTRAST
        );
        assertInvalid(result);
    }

    @CombinationTest
    public void sugo078(IAlgorithm algorithm, IPlayerBuilder builder) {
        {
            builder.allocate(SkillPoint.STRENGTH, 55);
            builder.allocate(SkillPoint.DEXTERITY, 34);
            builder.allocate(SkillPoint.DEFENCE, 34);
            builder.allocate(SkillPoint.AGILITY, 75);
            builder.equipment(
                Equipment.UNRAVEL,
                Equipment.WANDERLUST,
                Equipment.RUNEBOUND_CHAINS,
                Equipment.SKIDBLADNIR,
                Equipment.INGRESS,
                Equipment.INGRESS,
                Equipment.VORTEX_BRACER,
                Equipment.BOTTLED_THUNDERSTORM
            );
        }
        IPlayer player = builder.build();
        IAlgorithm.Result result = algorithm.run(player);
        assertValid(result,
            Equipment.UNRAVEL,
            Equipment.WANDERLUST,
            Equipment.RUNEBOUND_CHAINS,
            Equipment.SKIDBLADNIR,
            Equipment.INGRESS,
            Equipment.INGRESS,
            Equipment.VORTEX_BRACER,
            Equipment.BOTTLED_THUNDERSTORM
        );
        assertInvalid(result);
    }

}
