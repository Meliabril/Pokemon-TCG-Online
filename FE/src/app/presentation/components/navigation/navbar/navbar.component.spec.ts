import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { UserRole } from '../../../../core/models/enums/user/user-role.enum';
import { UserStatus } from '../../../../core/models/enums/user/user-status.enum';
import { STORAGE_KEYS } from '../../../../core/constants/storage/storage.constants';
import { StorageService } from '../../../../core/storage/storage.service';
import { AuthApiService } from '../../../../infrastructure/api/auth/auth-api.service';
import { NavbarComponent } from './navbar.component';

describe('NavbarComponent', () => {
  afterEach(() => {
    localStorage.clear();
  });

  it('shows guest navigation priorities', () => {
    const fixture = createComponent(false);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('NAV.HOME');
    expect(text).toContain('NAV.LOGIN');
    expect(text).toContain('NAV.REGISTER');
    expect(text).not.toContain('NAV.POKEDEX');
    expect(text).not.toContain('NAV.PLAY');
    expect(text).not.toContain('NAV.PROFILE');
  });

  it('shows authenticated navigation priorities', () => {
    const fixture = createComponent(true);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('NAV.DECK');
    expect(text).toContain('NAV.POKEDEX');
    expect(text).not.toContain('NAV.LOGIN');
    expect(text).not.toContain('NAV.REGISTER');
  });

  it('opens the user menu with aria-expanded enabled', () => {
    const fixture = createComponent(true);
    const menuTrigger = getUserMenuTrigger(fixture);

    menuTrigger.click();
    fixture.detectChanges();

    expect(menuTrigger.getAttribute('aria-expanded')).toBe('true');
    expect(fixture.nativeElement.querySelector('[role="menu"]')).not.toBeNull();
  });

  it('applies attached dropdown classes when the user menu is open', () => {
    const fixture = createComponent(true);
    const menuTrigger = getUserMenuTrigger(fixture);

    menuTrigger.click();
    fixture.detectChanges();

    expect(menuTrigger.className).toContain('rounded-b-none');
    expect(menuTrigger.className).toContain('rounded-t-[1.25rem]');
  });

  it('renders the dropdown menu attached to the trigger without the old gap class', () => {
    const fixture = createComponent(true);
    const menuTrigger = getUserMenuTrigger(fixture);

    menuTrigger.click();
    fixture.detectChanges();

    const menu = fixture.nativeElement.querySelector('[role="menu"]') as HTMLElement | null;

    expect(menu).not.toBeNull();
    expect(menu?.className).not.toContain('mt-2');
    expect(menu?.className).toContain('top-[calc(100%-1px)]');
    expect(menu?.className).toContain('w-full');
    expect(menu?.className).not.toContain('w-56');
  });

  it('toggles the persisted app theme from the navbar control', () => {
    const fixture = createComponent(false);
    const buttons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    const themeButton = buttons[1];
    const initialLabel = themeButton.getAttribute('aria-label');

    themeButton.click();
    fixture.detectChanges();

    expect(localStorage.getItem(STORAGE_KEYS.appTheme)).toBe('DARK');
    expect(themeButton.getAttribute('aria-label')).not.toBe(initialLabel);
  });

  function createComponent(isAuthenticated: boolean): ComponentFixture<NavbarComponent> {
    const authSignal = signal(isAuthenticated);
    const userSignal = signal(
      isAuthenticated
        ? {
            id: 'user-1',
            email: 'ash@kanto.dev',
            username: 'ash',
            role: UserRole.User,
            status: UserStatus.Active,
            emailVerified: true,
            createdAt: '2026-05-27T00:00:00Z',
            updatedAt: '2026-05-27T00:00:00Z'
          }
        : null
    );

    TestBed.configureTestingModule({
      imports: [NavbarComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthApiService,
          useValue: {
            logout: () => of({ message: 'ok' })
          }
        },
        {
          provide: StorageService,
          useValue: {
            clearSession: jasmine.createSpy('clearSession'),
            isAuthenticated: authSignal.asReadonly(),
            currentUser: userSignal.asReadonly()
          }
        }
      ]
    });

    const fixture = TestBed.createComponent(NavbarComponent);
    fixture.detectChanges();
    return fixture;
  }

  function getUserMenuTrigger(fixture: ComponentFixture<NavbarComponent>): HTMLButtonElement {
    return fixture.nativeElement.querySelector('button[aria-haspopup="menu"]') as HTMLButtonElement;
  }
});
