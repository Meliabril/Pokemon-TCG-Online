import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { MainLayoutComponent } from './presentation/layouts/main-layout/main-layout.component';
import { isTouchPortraitViewport } from './shared/utils/responsive-mode.util';

@Component({
  selector: 'app-root',
  imports: [MainLayoutComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class App implements OnInit, OnDestroy {
  private lastStableViewportHeight = 0;

  private readonly updateViewportSize = (): void => {
    if (typeof window === 'undefined') {
      return;
    }

    const viewport = window.visualViewport;
    const nextHeight = viewport?.height ?? window.innerHeight;
    const width = viewport?.width ?? window.innerWidth;
    const isMobilePortrait = isTouchPortraitViewport();
    const height = this.shouldPreserveGameViewportHeight(nextHeight)
      ? this.lastStableViewportHeight
      : nextHeight;

    if (height === nextHeight) {
      this.lastStableViewportHeight = nextHeight;
    }

    document.documentElement.style.setProperty('--app-height', `${height}px`);
    document.documentElement.style.setProperty('--app-width', `${width}px`);
    document.documentElement.classList.toggle('is-mobile-portrait', isMobilePortrait);
  };

  private shouldPreserveGameViewportHeight(nextHeight: number): boolean {
    if (this.lastStableViewportHeight <= 0 || nextHeight >= this.lastStableViewportHeight) {
      return false;
    }

    return (
      document.documentElement.classList.contains('game-route-active') &&
      window.matchMedia('(hover: none) and (pointer: coarse)').matches &&
      this.isTextEntryFocused(document.activeElement)
    );
  }

  private isTextEntryFocused(element: Element | null): boolean {
    if (!(element instanceof HTMLElement)) {
      return false;
    }

    const tagName = element.tagName.toLowerCase();
    return tagName === 'textarea' || tagName === 'select' || tagName === 'input' || element.isContentEditable;
  }

  private readonly handleOrientationChange = (): void => {
    window.scrollTo(0, 0);
    window.setTimeout(() => {
      this.updateViewportSize();
      window.scrollTo(0, 0);
    }, 250);
  };

  ngOnInit(): void {
    this.updateViewportSize();
    window.addEventListener('resize', this.updateViewportSize);
    window.addEventListener('orientationchange', this.handleOrientationChange);
    window.visualViewport?.addEventListener('resize', this.updateViewportSize);
  }

  ngOnDestroy(): void {
    window.removeEventListener('resize', this.updateViewportSize);
    window.removeEventListener('orientationchange', this.handleOrientationChange);
    window.visualViewport?.removeEventListener('resize', this.updateViewportSize);
  }
}
