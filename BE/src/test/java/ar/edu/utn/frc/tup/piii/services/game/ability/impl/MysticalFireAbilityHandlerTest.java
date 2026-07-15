package ar.edu.utn.frc.tup.piii.services.game.ability.impl;

import ar.edu.utn.frc.tup.piii.dtos.enums.AbilityCode;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameEventType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameEventDto;
import ar.edu.utn.frc.tup.piii.entities.Game;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.game.ability.AbilityResolution;
import ar.edu.utn.frc.tup.piii.services.game.ability.UseAbilityRequest;
import ar.edu.utn.frc.tup.piii.services.game.effect.DrawCardsEffectService;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameEventFactory;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MysticalFireAbilityHandlerTest {

    private static final int STATE_VERSION = 9;

    @Mock
    private DrawCardsEffectService drawCardsEffectService;

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private GameEventFactory gameEventFactory;

    private MysticalFireAbilityHandler handler;
    private UUID gameId;
    private UUID playerId;
    private UUID sourcePokemonId;
    private GameActionContext context;
    private PokemonInPlay sourcePokemon;

    @BeforeEach
    void setUp() {
        handler = new MysticalFireAbilityHandler(drawCardsEffectService, gameCardInstanceStateService, gameEventFactory);
        gameId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        sourcePokemonId = UUID.randomUUID();
        context = new GameActionContext(
                gameId,
                playerId,
                new GameActionRequestDto(gameId, UUID.randomUUID(), GameActionType.USE_ABILITY, 8, Map.of()),
                null);
        sourcePokemon = new PokemonInPlay();
        sourcePokemon.setId(sourcePokemonId);
        sourcePokemon.setOwnerUserId(playerId);

        lenient().when(gameEventFactory.publicEvent(eq(gameId), any(GameEventType.class), eq(STATE_VERSION), any()))
                .thenAnswer(invocation -> event(invocation.getArgument(1), invocation.getArgument(3), false));
        lenient().when(gameEventFactory.privateEvent(eq(gameId), any(GameEventType.class), eq(STATE_VERSION), any(), eq(playerId)))
                .thenAnswer(invocation -> event(invocation.getArgument(1), invocation.getArgument(3), true));
    }

    @Test
    void shouldDrawCardsUntilPlayerHasSixInHand() {
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerId, CardZone.HAND))
                .thenReturn(cards(2));
        when(drawCardsEffectService.drawUntilHandSize(gameId, playerId, 6))
                .thenReturn(cards(4));

        AbilityResolution resolution = handler.resolve(context, new Game(), playerId, sourcePokemon, request(), STATE_VERSION);

        assertThat(resolution.gameFinished()).isFalse();
        assertThat(resolution.promotionPending()).isFalse();
        assertThat(resolution.events()).hasSize(3);
        assertThat(resolution.events())
                .extracting(GameEventDto::eventType)
                .containsExactly(GameEventType.CARD_DRAWN, GameEventType.CARD_DRAWN, GameEventType.ATTACK_EFFECT_RESOLVED);
        assertThat(resolution.events().get(2).payload())
                .containsEntry("effectType", AbilityCode.MYSTICAL_FIRE.name())
                .containsEntry("pokemonInPlayId", sourcePokemonId.toString())
                .containsEntry("sourcePokemonId", sourcePokemonId.toString())
                .containsEntry("cardsDrawn", 4);
        verify(drawCardsEffectService).drawUntilHandSize(gameId, playerId, 6);
    }

    @Test
    void shouldDrawOnlyAvailableCardsReportedByDrawService() {
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerId, CardZone.HAND))
                .thenReturn(cards(3));
        when(drawCardsEffectService.drawUntilHandSize(gameId, playerId, 6))
                .thenReturn(cards(2));

        AbilityResolution resolution = handler.resolve(context, new Game(), playerId, sourcePokemon, request(), STATE_VERSION);

        assertThat(resolution.events().get(2).payload()).containsEntry("cardsDrawn", 2);
    }

    @Test
    void shouldRejectWhenPlayerAlreadyHasSixCardsInHand() {
        when(gameCardInstanceStateService.findByGameIdAndOwnerUserIdAndZone(gameId, playerId, CardZone.HAND))
                .thenReturn(cards(6));

        assertThatThrownBy(() -> handler.resolve(context, new Game(), playerId, sourcePokemon, request(), STATE_VERSION))
                .isInstanceOf(InvalidGameActionException.class)
                .hasMessageContaining("6 o mas cartas");
        verifyNoInteractions(drawCardsEffectService);
    }

    private UseAbilityRequest request() {
        return new UseAbilityRequest(sourcePokemonId, AbilityCode.MYSTICAL_FIRE, null, null, null, null, null, null);
    }

    private List<GameCardInstance> cards(int amount) {
        return java.util.stream.IntStream.range(0, amount)
                .mapToObj(index -> {
                    GameCardInstance card = new GameCardInstance();
                    card.setId(UUID.randomUUID());
                    card.setCardId(UUID.randomUUID());
                    return card;
                })
                .toList();
    }

    private GameEventDto event(GameEventType eventType, Map<String, Object> payload, boolean privateEvent) {
        return new GameEventDto(UUID.randomUUID(), gameId, eventType, STATE_VERSION, privateEvent, Instant.now(), payload);
    }
}
