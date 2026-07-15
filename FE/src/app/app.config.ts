import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { authInterceptor } from './core/interceptors/auth/auth.interceptor';
import { LanguageService } from './core/services/language.service';
import { AuthApiService } from './infrastructure/api/auth/auth-api.service';
import { routes } from './routes/app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAppInitializer(() => inject(LanguageService).initialize()),
    provideAppInitializer(() => firstValueFrom(inject(AuthApiService).initializeSession())),
    provideRouter(routes)
  ]
};
