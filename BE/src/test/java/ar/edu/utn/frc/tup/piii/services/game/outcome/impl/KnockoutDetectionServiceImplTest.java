package ar.edu.utn.frc.tup.piii.services.game.outcome.impl;

import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnockoutDetectionServiceImplTest {

    @Mock
    private CardService cardService;

    @InjectMocks
    private KnockoutDetectionServiceImpl service;

    @Test
    void isKnockedOut_nullPokemon_returnsFalse() {
        assertThat(service.isKnockedOut(null)).isFalse();
    }

    @Test
    void isKnockedOut_noActiveCardInstance_returnsFalse() {
        PokemonInPlay pip = new PokemonInPlay();
        pip.setActiveCardInstance(null);
        assertThat(service.isKnockedOut(pip)).isFalse();
    }

    @Test
    void isKnockedOut_cardWithNullHp_returnsFalse() {
        UUID cardId = UUID.randomUUID();
        GameCardInstance instance = new GameCardInstance();
        instance.setCardId(cardId);
        PokemonInPlay pip = new PokemonInPlay();
        pip.setActiveCardInstance(instance);
        pip.setDamageCounters(10);

        Card card = new Card();
        card.setHp(null);
        when(cardService.getCardEntityById(cardId)).thenReturn(card);

        assertThat(service.isKnockedOut(pip)).isFalse();
    }

    @Test
    void isKnockedOut_damageEqualsHp_returnsTrue() {
        UUID cardId = UUID.randomUUID();
        GameCardInstance instance = new GameCardInstance();
        instance.setCardId(cardId);
        PokemonInPlay pip = new PokemonInPlay();
        pip.setActiveCardInstance(instance);
        pip.setDamageCounters(6); // 6 * 10 = 60

        Card card = new Card();
        card.setHp(60);
        when(cardService.getCardEntityById(cardId)).thenReturn(card);

        assertThat(service.isKnockedOut(pip)).isTrue();
    }

    @Test
    void isKnockedOut_damageExceedsHp_returnsTrue() {
        UUID cardId = UUID.randomUUID();
        GameCardInstance instance = new GameCardInstance();
        instance.setCardId(cardId);
        PokemonInPlay pip = new PokemonInPlay();
        pip.setActiveCardInstance(instance);
        pip.setDamageCounters(12); // 12 * 10 = 120 > 100

        Card card = new Card();
        card.setHp(100);
        when(cardService.getCardEntityById(cardId)).thenReturn(card);

        assertThat(service.isKnockedOut(pip)).isTrue();
    }

    @Test
    void isKnockedOut_damageBelowHp_returnsFalse() {
        UUID cardId = UUID.randomUUID();
        GameCardInstance instance = new GameCardInstance();
        instance.setCardId(cardId);
        PokemonInPlay pip = new PokemonInPlay();
        pip.setActiveCardInstance(instance);
        pip.setDamageCounters(3); // 3 * 10 = 30 < 100

        Card card = new Card();
        card.setHp(100);
        when(cardService.getCardEntityById(cardId)).thenReturn(card);

        assertThat(service.isKnockedOut(pip)).isFalse();
    }

    @Test
    void isKnockedOut_nullDamageCounters_returnsFalse() {
        UUID cardId = UUID.randomUUID();
        GameCardInstance instance = new GameCardInstance();
        instance.setCardId(cardId);
        PokemonInPlay pip = new PokemonInPlay();
        pip.setActiveCardInstance(instance);
        pip.setDamageCounters(null);

        Card card = new Card();
        card.setHp(60);
        when(cardService.getCardEntityById(cardId)).thenReturn(card);

        assertThat(service.isKnockedOut(pip)).isFalse();
    }
}
