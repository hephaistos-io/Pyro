import {ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners} from '@angular/core';
import {provideRouter} from '@angular/router';
import {provideHttpClient} from '@angular/common/http';
import {routes} from './app.routes';
import {ApiConfiguration} from './api/generated/api-configuration';
import {environment} from '../environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(),
    provideAppInitializer(() => {
      const config = inject(ApiConfiguration);
      config.rootUrl = environment.apiUrl;
    })
  ]
};
