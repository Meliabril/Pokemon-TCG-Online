import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('renders the main layout with the navbar shell', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const nativeElement = fixture.nativeElement as HTMLElement;

    expect(nativeElement.querySelector('app-main-layout')).not.toBeNull();
    expect(nativeElement.querySelector('app-navbar')).not.toBeNull();
    expect(nativeElement.querySelector('router-outlet')).not.toBeNull();
  });
});
