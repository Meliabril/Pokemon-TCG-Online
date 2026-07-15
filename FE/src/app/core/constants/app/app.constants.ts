import { environment } from '../../../../environments/environment';

export const APP_CONFIG = {
  appName: 'Pokemon TCG',
  production: environment.production,
  apiBaseUrl: environment.apiBaseUrl,
  wsBaseUrl: environment.wsBaseUrl
} as const;
