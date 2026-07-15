import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LanguageService } from '../../../../core/services/language.service';
import { HandFanComponent } from './hand-fan.component';

describe('HandFanComponent mobile primary action', () => {
  let fixture: ComponentFixture<HandFanComponent>;
  let component: HandFanComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HandFanComponent],
      providers: [
        {
          provide: LanguageService,
          useValue: {
            t: (key: string) => ({
              'GAME.INTERACTION.SEND_SETUP': 'Enviar setup',
              'GAME.WAITING_FOR_OPPONENT': 'Esperando rival',
              'GAME.END_TURN': 'Terminar turno',
              'GAME.SETUP_MOBILE_HELP': 'Ayuda de setup',
              'GAME.CLEAR_BENCH': 'Borrar banca',
              'GAME.YOUR_HAND': 'Tu mano',
              'GAME.HAND_OPEN': 'Abrir',
              'GAME.NO_AVAILABLE': 'No disponible'
            })[key] ?? key
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HandFanComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('label', 'Tu mano');
    fixture.componentRef.setInput('cards', []);
    fixture.componentRef.setInput('hoverable', true);
  });

  it('sends setup from the lower action without ending the turn', () => {
    const setupSpy = spyOn(component.setupSubmitRequested, 'emit');
    const endTurnSpy = spyOn(component.endTurnRequested, 'emit');
    fixture.componentRef.setInput('setupPhase', true);
    fixture.componentRef.setInput('setupSubmitEnabled', true);
    fixture.detectChanges();

    const button = primaryAction();
    expect(button.textContent).toContain('Enviar setup');
    button.click();

    expect(setupSpy).toHaveBeenCalledOnceWith();
    expect(endTurnSpy).not.toHaveBeenCalled();
  });

  it('does not allow setup submission before choosing an active Pokemon', () => {
    const setupSpy = spyOn(component.setupSubmitRequested, 'emit');
    fixture.componentRef.setInput('setupPhase', true);
    fixture.componentRef.setInput('setupSubmitEnabled', false);
    fixture.componentRef.setInput('setupDisabledReason', 'Primero elegí un Pokémon básico como activo.');
    fixture.detectChanges();

    const button = primaryAction();
    expect(button.disabled).toBeTrue();
    expect(button.title).toBe('Primero elegí un Pokémon básico como activo.');
    button.click();
    expect(setupSpy).not.toHaveBeenCalled();
  });

  it('shows the waiting state and prevents duplicate setup submission', () => {
    fixture.componentRef.setInput('setupPhase', true);
    fixture.componentRef.setInput('setupSubmitted', true);
    fixture.componentRef.setInput('setupSubmitEnabled', false);
    fixture.detectChanges();

    const button = primaryAction();
    expect(button.textContent).toContain('Esperando rival');
    expect(button.disabled).toBeTrue();
  });

  it('allows clearing selected bench Pokemon during setup', () => {
    const clearBenchSpy = spyOn(component.setupBenchClearRequested, 'emit');
    fixture.componentRef.setInput('setupPhase', true);
    fixture.componentRef.setInput('setupBenchCardInstanceIds', ['bench-card-1']);
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('[data-testid="mobile-clear-bench"]') as HTMLButtonElement;
    expect(button.disabled).toBeFalse();
    button.click();

    expect(clearBenchSpy).toHaveBeenCalledOnceWith();
  });

  it('ends the turn from the same lower action during active play', () => {
    const setupSpy = spyOn(component.setupSubmitRequested, 'emit');
    const endTurnSpy = spyOn(component.endTurnRequested, 'emit');
    fixture.componentRef.setInput('setupPhase', false);
    fixture.componentRef.setInput('endTurnEnabled', true);
    fixture.detectChanges();

    const button = primaryAction();
    expect(button.textContent).toContain('Terminar turno');
    button.click();

    expect(endTurnSpy).toHaveBeenCalledOnceWith();
    expect(setupSpy).not.toHaveBeenCalled();
  });

  function primaryAction(): HTMLButtonElement {
    const button = fixture.nativeElement.querySelector('[data-testid="mobile-primary-action"]') as HTMLButtonElement | null;
    if (!button) {
      throw new Error('Mobile primary action was not rendered');
    }
    return button;
  }
});
