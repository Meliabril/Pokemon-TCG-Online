export const DESKTOP_INPUT_MEDIA_QUERY =
  '(hover: hover) and (pointer: fine), (any-hover: hover) and (any-pointer: fine)';
export const TOUCH_INPUT_MEDIA_QUERY =
  '(hover: none) and (pointer: coarse) and (any-hover: none) and (any-pointer: coarse)';
export const MOBILE_LANDSCAPE_MEDIA_QUERY =
  '(max-width: 932px) and (orientation: landscape) and (hover: none) and (pointer: coarse) and (any-hover: none) and (any-pointer: coarse)';
const LANDSCAPE_MEDIA_QUERY = '(orientation: landscape)';

function matchesMedia(query: string): boolean {
  return typeof window !== 'undefined' && window.matchMedia(query).matches;
}

export function isDesktopViewport(): boolean {
  return matchesMedia(DESKTOP_INPUT_MEDIA_QUERY);
}

export function isTouchViewport(): boolean {
  return matchesMedia(TOUCH_INPUT_MEDIA_QUERY);
}

export function isLandscapeViewport(): boolean {
  return matchesMedia(LANDSCAPE_MEDIA_QUERY);
}

export function isMobileLandscapeViewport(): boolean {
  return matchesMedia(MOBILE_LANDSCAPE_MEDIA_QUERY);
}

export function isTouchPortraitViewport(): boolean {
  return isTouchViewport() && !isLandscapeViewport();
}
