package ar.edu.utn.frc.tup.piii.services.game.trainer.impl;

import ar.edu.utn.frc.tup.piii.support.GameStateTestFactory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardCategory;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardSupertype;
import ar.edu.utn.frc.tup.piii.dtos.enums.CardZone;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.enums.GameStatus;
import ar.edu.utn.frc.tup.piii.dtos.enums.TurnPhase;
import ar.edu.utn.frc.tup.piii.dtos.game.GameActionRequestDto;
import ar.edu.utn.frc.tup.piii.dtos.game.GameStateDto;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.GameCardInstance;
import ar.edu.utn.frc.tup.piii.services.game.state.GameCardInstanceStateService;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectContext;
import ar.edu.utn.frc.tup.piii.services.game.trainer.TrainerEffectResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map;
import java.util.UUID;
import ar.edu.utn.frc.tup.piii.services.card.CardService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayStadiumTrainerEffectTest {

    @Mock
    private GameCardInstanceStateService gameCardInstanceStateService;

    @Mock
    private CardService cardService;

    @InjectMocks
    private PlayStadiumTrainerEffect effect;

    // ─── supports ─────────────────────────────────────────────────────────────

    @Test
    void supports_nullCard_returnsFalse() {
        assertThat(effect.supports(null)).isFalse();
    }

    @Test
    void supports_itemTrainer_returnsFalse() {
        assertThat(effect.supports(card(CardCategory.ITEM_TRAINER))).isFalse();
    }

    @Test
    void supports_supporterTrainer_returnsFalse() {
        assertThat(effect.supports(card(CardCategory.SUPPORTER_TRAINER))).isFalse();
    }

    @Test
    void supports_stadiumTrainer_returnsTrue() {
        assertThat(effect.supports(card(CardCategory.STADIUM_TRAINER))).isTrue();
    }

    // ─── keepsCardInPlay ──────────────────────────────────────────────────────

    @Test
    void keepsCardInPlay_returnsTrue() {
        assertThat(effect.keepsCardInPlay()).isTrue();
    }

    // ─── apply ────────────────────────────────────────────────────────────────

    @Test
    void apply_noExistingStadium_placesCardInStadiumZone() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card stadiumCard = card(CardCategory.STADIUM_TRAINER);
        stadiumCard.setExternalId("xy1-117");
        GameCardInstance trainerInstance = cardInstance(UUID.randomUUID(), actorUserId, stadiumCard.getId(), CardZone.HAND, 1);

        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of());

        TrainerEffectResult result = effect.apply(context(gameId, actorUserId, stadiumCard, trainerInstance));

        assertThat(trainerInstance.getZone()).isEqualTo(CardZone.STADIUM);
        assertThat(trainerInstance.getZonePosition()).isEqualTo(1);
        assertThat(trainerInstance.getFaceDown()).isFalse();
        assertThat(result.effectData()).containsEntry("effectType", "PLAY_STADIUM");
        assertThat(result.effectData()).containsEntry("cardExternalId", "xy1-117");
        assertThat(result.emittedEvents()).isEmpty();
        verify(gameCardInstanceStateService).save(trainerInstance);
        verify(gameCardInstanceStateService).resequenceZone(gameId, actorUserId, CardZone.HAND);
    }

    @Test
    void apply_existingStadium_movesOldToDiscardFirst() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID oldOwnerUserId = UUID.randomUUID();
        Card stadiumCard = card(CardCategory.STADIUM_TRAINER);
        stadiumCard.setExternalId("xy1-126");
        GameCardInstance trainerInstance = cardInstance(UUID.randomUUID(), actorUserId, stadiumCard.getId(), CardZone.HAND, 1);

        GameCardInstance oldStadium = cardInstance(UUID.randomUUID(), oldOwnerUserId, UUID.randomUUID(), CardZone.STADIUM, 1);
        Card oldCard = card(CardCategory.STADIUM_TRAINER);

        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(oldStadium));
        when(gameCardInstanceStateService.nextZonePosition(gameId, oldOwnerUserId, CardZone.DISCARD)).thenReturn(5);
        when(cardService.getCardEntityById(oldStadium.getCardId())).thenReturn(oldCard);

        TrainerEffectResult result = effect.apply(context(gameId, actorUserId, stadiumCard, trainerInstance));

        assertThat(oldStadium.getZone()).isEqualTo(CardZone.DISCARD);
        assertThat(oldStadium.getZonePosition()).isEqualTo(5);
        assertThat(trainerInstance.getZone()).isEqualTo(CardZone.STADIUM);
        assertThat(result.effectData()).containsEntry("effectType", "PLAY_STADIUM");
        assertThat(result.effectData()).containsEntry("cardExternalId", "xy1-126");
        verify(gameCardInstanceStateService).save(oldStadium);
        verify(gameCardInstanceStateService).save(trainerInstance);
    }

    @Test
    void apply_nonStadiumCardsInPlayAreIgnored() {
        UUID gameId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        Card stadiumCard = card(CardCategory.STADIUM_TRAINER);
        stadiumCard.setExternalId("xy1-117");
        GameCardInstance trainerInstance = cardInstance(UUID.randomUUID(), actorUserId, stadiumCard.getId(), CardZone.HAND, 1);

        GameCardInstance handCard = cardInstance(UUID.randomUUID(), actorUserId, UUID.randomUUID(), CardZone.HAND, 2);

        when(gameCardInstanceStateService.findByGameId(gameId)).thenReturn(List.of(handCard));

        effect.apply(context(gameId, actorUserId, stadiumCard, trainerInstance));

        // Old hand card must NOT be moved to DISCARD
        assertThat(handCard.getZone()).isEqualTo(CardZone.HAND);
        verify(gameCardInstanceStateService, never()).nextZonePosition(gameId, actorUserId, CardZone.DISCARD);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private Card card(CardCategory category) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setSupertype(CardSupertype.TRAINER);
        card.setCategory(category);
        card.setExternalId("test-" + UUID.randomUUID());
        return card;
    }

    private GameCardInstance cardInstance(UUID id, UUID ownerUserId, UUID cardId, CardZone zone, int position) {
        GameCardInstance instance = new GameCardInstance();
        instance.setId(id);
        instance.setOwnerUserId(ownerUserId);
        instance.setCardId(cardId);
        instance.setZone(zone);
        instance.setZonePosition(position);
        instance.setFaceDown(false);
        return instance;
    }

    private TrainerEffectContext context(UUID gameId, UUID actorUserId, Card trainerCard, GameCardInstance trainerInstance) {
        GameStateDto state = GameStateTestFactory.state(
                gameId,
                GameStatus.ACTIVE,
                TurnPhase.MAIN,
                3,
                6,
                actorUserId,
                List.of(GameActionType.PLAY_TRAINER),
                Instant.parse("2026-05-24T12:00:00Z"));
        GameActionRequestDto request = new GameActionRequestDto(
                gameId,
                UUID.randomUUID(),
                GameActionType.PLAY_TRAINER,
                6,
                Map.<String, Object>of("cardId", trainerCard.getId().toString()));
        return new TrainerEffectContext(
                gameId,
                actorUserId,
                request,
                state,
                trainerInstance,
                trainerCard,
                7);
    }
}
