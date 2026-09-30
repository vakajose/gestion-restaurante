import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

// Bootstrap de aplicación Angular 22 Zoneless
bootstrapApplication(AppComponent, appConfig)
  .catch((err: unknown) => console.error(err));
