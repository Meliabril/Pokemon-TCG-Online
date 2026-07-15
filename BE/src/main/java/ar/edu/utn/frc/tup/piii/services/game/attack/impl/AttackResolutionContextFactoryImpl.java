package ar.edu.utn.frc.tup.piii.services.game.attack.impl;

import ar.edu.utn.frc.tup.piii.entities.Attack;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidGameActionException;
import ar.edu.utn.frc.tup.piii.services.card.CardService;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContext;
import ar.edu.utn.frc.tup.piii.services.game.attack.AttackResolutionContextFactory;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionPayloadReader;
import ar.edu.utn.frc.tup.piii.services.game.state.GameParticipantStateService;
import ar.edu.utn.frc.tup.piii.services.game.state.PokemonInPlayStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttackResolutionContextFactoryImpl implements AttackResolutionContextFactory {

    private static final String ATTACK_ID_KEY = "attackId";

    private final GameActionPayloadReader payloadReader;
    private final PokemonInPlayStateService pokemonInPlayStateService;
    private final GameParticipantStateService gameParticipantStateService;
    private final CardService cardService;

    @Override
    public AttackResolutionContext create(GameActionContext context) {
        UUID gameId = context.gameId();
        UUID attackerUserId = context.actorUserId();
        UUID attackId = payloadReader.requiredUuid(context.request().payload(), ATTACK_ID_KEY);

        PokemonInPlay attackerPokemon = activePokemon(gameId, attackerUserId, "Active attacking Pokemon was not found");
        UUID defenderUserId = gameParticipantStateService.findOpponentUserId(gameId, attackerUserId);
        PokemonInPlay defenderPokemon = activePokemon(gameId, defenderUserId, "Defending active Pokemon was not found");

        Card attackerCard = cardService.getCardEntityById(attackerPokemon.getActiveCardInstance().getCardId());
        Card defenderCard = cardService.getCardEntityById(defenderPokemon.getActiveCardInstance().getCardId());
        Attack selectedAttack = findAttack(attackerCard, attackId);

        return new AttackResolutionContext(
                gameId,
                attackerUserId,
                defenderUserId,
                attackerPokemon,
                defenderPokemon,
                attackerCard,
                defenderCard,
                selectedAttack);
    }

    private PokemonInPlay activePokemon(UUID gameId, UUID ownerUserId, String message) {
        Optional<PokemonInPlay> activePokemon = pokemonInPlayStateService.findActivePokemon(gameId, ownerUserId);
        if (activePokemon.isEmpty()) {
            throw new InvalidGameActionException(message);
        }

        return activePokemon.get();
    }

    private Attack findAttack(Card attackerCard, UUID attackId) {
        if (attackerCard.getAttacks() != null) {
            for (Attack attack : attackerCard.getAttacks()) {
                if (attack != null && attackId.equals(attack.getId())) {
                    return attack;
                }
            }
        }

        throw new InvalidGameActionException("Selected attack was not found on the active Pokemon");
    }
}
