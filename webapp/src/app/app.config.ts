import {ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners} from '@angular/core';
import {provideRouter, withInMemoryScrolling} from '@angular/router';
import {provideHttpClient, withInterceptors} from '@angular/common/http';
import {routes} from './app.routes';
import {ApiConfiguration} from './api/generated/api-configuration';
import {environment} from '../environments/environment';
import {authInterceptor} from './interceptors/auth.interceptor';
import {traceInterceptor} from './interceptors/trace.interceptor';
import {ThemeService} from './services/theme.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withInMemoryScrolling({anchorScrolling: 'enabled', scrollPositionRestoration: 'enabled'})),
    provideHttpClient(withInterceptors([traceInterceptor, authInterceptor])),
    provideAppInitializer(() => {
      const config = inject(ApiConfiguration);
      config.rootUrl = environment.apiUrl;
    }),
    provideAppInitializer(() => {
      // Initialize ThemeService early to apply saved theme preference
      inject(ThemeService);
    })
  ]
};
