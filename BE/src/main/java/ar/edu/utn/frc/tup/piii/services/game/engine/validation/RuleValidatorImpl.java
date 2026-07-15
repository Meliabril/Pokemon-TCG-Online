package ar.edu.utn.frc.tup.piii.services.game.engine.validation;

import ar.edu.utn.frc.tup.piii.services.game.engine.GameActionContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class RuleValidatorImpl implements RuleValidator {

    private final List<ActionValidator> actionValidators;

    public RuleValidatorImpl(List<ActionValidator> actionValidators) {
        this.actionValidators = List.copyOf(Objects.requireNonNull(actionValidators, "Action validators are required"));
    }

    @Override
    public void validate(GameActionContext context) {
        for (ActionValidator actionValidator : actionValidators) {
            actionValidator.validate(context);
        }
    }
}
