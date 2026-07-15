package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AttachedCardType;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonAttachedCard;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonAttachedCardStateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PokemonToolModifierServiceImplTest {

    @Mock
    private PokemonAttachedCardStateService pokemonAttachedCardStateService;

    @Mock
    private CardService cardService;

    @InjectMocks
    private PokemonToolModifierServiceImpl service;

    // ─── getOutgoingDamageBonus ───────────────────────────────────────────────

    @Test
    void getOutgoingDamageBonus_nullAttacker_returnsZero() {
        assertThat(service.getOutgoingDamageBonus(null)).isEqualTo(0);
    }

    @Test
    void getOutgoingDamageBonus_noAttachedCards_returnsZero() {
        PokemonInPlay attacker = pokemon();
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(attacker.getId())).thenReturn(List.of());

        assertThat(service.getOutgoingDamageBonus(attacker)).isEqualTo(0);
    }

    @Test
    void getOutgoingDamageBonus_energyAttachedNotTool_returnsZero() {
        PokemonInPlay attacker = pokemon();
        PokemonAttachedCard energy = attachedCard(AttachedCardType.BASIC_ENERGY, "xy1-121");

        when(pokemonAttachedCardStateService.findByPokemonInPlayId(attacker.getId())).thenReturn(List.of(energy));

        assertThat(service.getOutgoingDamageBonus(attacker)).isEqualTo(0);
    }

    @Test
    void getOutgoingDamageBonus_toolAttachedButNotMuscleBand_returnsZero() {
        PokemonInPlay attacker = pokemon();
        PokemonAttachedCard tool = attachedCard(AttachedCardType.POKEMON_TOOL, "xy1-119");

        when(pokemonAttachedCardStateService.findByPokemonInPlayId(attacker.getId())).thenReturn(List.of(tool));
        when(cardService.getCardEntityById(tool.getGameCardInstance().getCardId())).thenReturn(cardWithExternalId("xy1-119"));

        assertThat(service.getOutgoingDamageBonus(attacker)).isEqualTo(0);
    }

    @Test
    void getOutgoingDamageBonus_muscleBandAttached_returns20() {
        PokemonInPlay attacker = pokemon();
        PokemonAttachedCard tool = attachedCard(AttachedCardType.POKEMON_TOOL, "xy1-121");

        when(pokemonAttachedCardStateService.findByPokemonInPlayId(attacker.getId())).thenReturn(List.of(tool));
        when(cardService.getCardEntityById(tool.getGameCardInstance().getCardId())).thenReturn(cardWithExternalId("xy1-121"));

        assertThat(service.getOutgoingDamageBonus(attacker)).isEqualTo(20);
    }

    // ─── getIncomingDamageModifier ────────────────────────────────────────────

    @Test
    void getIncomingDamageModifier_nullDefender_returnsZero() {
        assertThat(service.getIncomingDamageModifier(null)).isEqualTo(0);
    }

    @Test
    void getIncomingDamageModifier_noAttachedCards_returnsZero() {
        PokemonInPlay defender = pokemon();
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(defender.getId())).thenReturn(List.of());

        assertThat(service.getIncomingDamageModifier(defender)).isEqualTo(0);
    }

    @Test
    void getIncomingDamageModifier_energyAttachedNotTool_returnsZero() {
        PokemonInPlay defender = pokemon();
        PokemonAttachedCard energy = attachedCard(AttachedCardType.BASIC_ENERGY, "xy1-119");

        when(pokemonAttachedCardStateService.findByPokemonInPlayId(defender.getId())).thenReturn(List.of(energy));

        assertThat(service.getIncomingDamageModifier(defender)).isEqualTo(0);
    }

    @Test
    void getIncomingDamageModifier_toolAttachedButNotHardCharm_returnsZero() {
        PokemonInPlay defender = pokemon();
        PokemonAttachedCard tool = attachedCard(AttachedCardType.POKEMON_TOOL, "xy1-121");

        when(pokemonAttachedCardStateService.findByPokemonInPlayId(defender.getId())).thenReturn(List.of(tool));
        when(cardService.getCardEntityById(tool.getGameCardInstance().getCardId())).thenReturn(cardWithExternalId("xy1-121"));

        assertThat(service.getIncomingDamageModifier(defender)).isEqualTo(0);
    }

    @Test
    void getIncomingDamageModifier_hardCharmAttached_returnsMinus20() {
        PokemonInPlay defender = pokemon();
        PokemonAttachedCard tool = attachedCard(AttachedCardType.POKEMON_TOOL, "xy1-119");

        when(pokemonAttachedCardStateService.findByPokemonInPlayId(defender.getId())).thenReturn(List.of(tool));
        when(cardService.getCardEntityById(tool.getGameCardInstance().getCardId())).thenReturn(cardWithExternalId("xy1-119"));

        assertThat(service.getIncomingDamageModifier(defender)).isEqualTo(-20);
    }

    @Test
    void getIncomingDamageModifier_multiplePokemonTools_firstMatchWins() {
        PokemonInPlay defender = pokemon();
        PokemonAttachedCard hardCharm = attachedCard(AttachedCardType.POKEMON_TOOL, "xy1-119");
        PokemonAttachedCard otherTool = attachedCard(AttachedCardType.POKEMON_TOOL, "xy1-121");

        when(pokemonAttachedCardStateService.findByPokemonInPlayId(defender.getId()))
                .thenReturn(List.of(hardCharm, otherTool));
        when(cardService.getCardEntityById(hardCharm.getGameCardInstance().getCardId())).thenReturn(cardWithExternalId("xy1-119"));
        when(cardService.getCardEntityById(otherTool.getGameCardInstance().getCardId())).thenReturn(cardWithExternalId("xy1-121"));

        assertThat(service.getIncomingDamageModifier(defender)).isEqualTo(-20);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private PokemonInPlay pokemon() {
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setId(UUID.randomUUID());
        return pokemon;
    }

    private PokemonAttachedCard attachedCard(AttachedCardType type, String externalId) {
        GameCardInstance gci = new GameCardInstance();
        gci.setId(UUID.randomUUID());
        gci.setCardId(UUID.randomUUID());

        PokemonAttachedCard attached = new PokemonAttachedCard();
        attached.setId(UUID.randomUUID());
        attached.setAttachedCardType(type);
        attached.setGameCardInstance(gci);
        return attached;
    }

    private Card cardWithExternalId(String externalId) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setExternalId(externalId);
        return card;
    }

    @Test
    void getOutgoingDamageBonus_hardCharmOnlyAttached_returnsZero() {
        PokemonInPlay attacker = pokemon();
        PokemonAttachedCard tool = attachedCard(AttachedCardType.POKEMON_TOOL, "xy1-119");
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(attacker.getId())).thenReturn(List.of(tool));
        when(cardService.getCardEntityById(tool.getGameCardInstance().getCardId())).thenReturn(cardWithExternalId("xy1-119"));
        assertThat(service.getOutgoingDamageBonus(attacker)).isEqualTo(0);
    }

    @Test
    void getOutgoingDamageBonus_multipleTools_muscleBandPresentSecond_returns20() {
        PokemonInPlay attacker = pokemon();
        PokemonAttachedCard hardCharm = attachedCard(AttachedCardType.POKEMON_TOOL, "xy1-119");
        PokemonAttachedCard muscleBand = attachedCard(AttachedCardType.POKEMON_TOOL, "xy1-121");
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(attacker.getId()))
                .thenReturn(List.of(hardCharm, muscleBand));
        when(cardService.getCardEntityById(hardCharm.getGameCardInstance().getCardId())).thenReturn(cardWithExternalId("xy1-119"));
        when(cardService.getCardEntityById(muscleBand.getGameCardInstance().getCardId())).thenReturn(cardWithExternalId("xy1-121"));
        assertThat(service.getOutgoingDamageBonus(attacker)).isEqualTo(20);
    }

    @Test
    void getIncomingDamageModifier_muscleBandOnlyAttached_returnsZero() {
        PokemonInPlay defender = pokemon();
        PokemonAttachedCard tool = attachedCard(AttachedCardType.POKEMON_TOOL, "xy1-121");
        when(pokemonAttachedCardStateService.findByPokemonInPlayId(defender.getId())).thenReturn(List.of(tool));
        when(cardService.getCardEntityById(tool.getGameCardInstance().getCardId())).thenReturn(cardWithExternalId("xy1-121"));
        assertThat(service.getIncomingDamageModifier(defender)).isEqualTo(0);
    }
}
