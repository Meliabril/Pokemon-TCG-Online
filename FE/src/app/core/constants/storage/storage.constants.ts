export const STORAGE_KEYS = {
  language: 'pokemon-tcg-language',
  appTheme: 'pokemon-auth-theme'
} as const;

/**
 * Legacy keys that older versions of the app used to persist the auth
 * session/tokens in `localStorage`/`sessionStorage`. They are no longer
 * written to (see StorageService), but are kept here so any leftover values
 * from before that fix can be proactively purged from returning users'
 * browsers.
 */
export const LEGACY_AUTH_STORAGE_KEYS = [
  'pokemon-tcg.auth.session',
  'pokemon-tcg.auth.access-token',
  'pokemon-tcg.auth.refresh-token'
] as const;
