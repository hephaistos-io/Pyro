# Webapp - Coding Guidelines

> For setup, running, testing, and Docker commands, see `doc/src/subchapters/setup.adoc`

## Philosophy

Prefer **readability and simplicity over cleverness**. Always ask before making big architectural changes.

**Design Inspiration:** www.enode.com

---

## Project Structure

```
src/app/
├── api/generated/     # Auto-generated API client (ng-openapi-gen)
├── components/        # Reusable UI components
├── guards/            # Route guards (authGuard)
├── interceptors/      # HTTP interceptors (authInterceptor)
├── layouts/           # Page layout wrappers
├── pages/             # Route-level page components
├── services/          # Singleton services (providedIn: 'root')
├── utils/             # Utility functions
├── app.routes.ts      # Route definitions
└── app.config.ts      # Application configuration

src/styles/            # Global SCSS system
├── _index.scss        # Barrel file (import this in components)
├── _variables.scss    # Colors, fonts, dark mode variables
├── _mixins.scss       # Reusable SCSS mixins (dark-mode, etc.)
└── _buttons.scss      # Button component styles
```

---

## Component Conventions

### File Structure

```
component-name/
├── component-name.component.ts
├── component-name.component.html
└── component-name.component.scss
```

### Basic Pattern

```typescript
import { Component, inject } from '@angular/core';

@Component({
  selector: 'app-component-name',
  standalone: true,
  imports: [],
  templateUrl: './component-name.component.html',
  styleUrl: './component-name.component.scss'  // singular
})
export class ComponentNameComponent {
  private someService = inject(SomeService);  // Use inject(), not constructor
}
```

### Naming

- Selector: `app-{component-name}` (kebab-case)
- Class: `{ComponentName}Component` (PascalCase)
- Files: `{component-name}.component.{ts,html,scss}` (kebab-case)

---

## Angular Patterns

### Signals (prefer over RxJS for simple state)

```typescript
import { signal, computed } from '@angular/core';

isDarkMode = signal(false);
greeting = computed(() => this.user() ? `Hello, ${this.user().name}` : 'Hello');
```

### Modern Template Syntax

```html
@if (condition) {
  <div>Shown when true</div>
} @else {
  <div>Shown when false</div>
}

@for (item of items; track item.id) {
  <div>{{ item.name }}</div>
}
```

### RxJS Cleanup

```typescript
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

constructor() {
  someObservable$.pipe(takeUntilDestroyed()).subscribe(...);
}
```

---

## SCSS Guidelines

### Import Pattern

```scss
@use '../../../styles/index' as *;

.component {
  background: $pyro-red;
  color: $charcoal;
}
```

### Dark Mode

```scss
.element {
  background: $white;
  @include dark-mode {
    background: $dark-card;
  }
}
```

### BEM Naming

```scss
.card { }
.card__title { }
.card--highlighted { }
```

### Available Variables

- Colors: `$white`, `$charcoal`, `$off-white`, `$warm-gray`
- Brand: `$pyro-red`, `$pyro-red-light`, `$pyro-red-pale`, `$pyro-red-deep`
- Dark mode: `$dark-bg`, `$dark-card`, `$dark-border`, `$dark-text`
- Fonts: `$font-serif` (headings), `$font-sans` (body)

---

## API Client

Location: `src/app/api/generated/` (auto-generated from OpenAPI spec)

```typescript
import { Api } from '../api/generated/api';

private api = inject(Api);

async loadData() {
  const response = await this.api.getApplications();
}
```

Regenerate after backend changes:

```bash
./gradlew webapp:generateTypeScriptClient
```

---

## Do's and Don'ts

**Do:**

- Use `inject()` function, not constructor injection
- Use `@use` in SCSS (not `@import`)
- Use `track` in all `@for` loops
- Use variables from `_variables.scss`
- Test both light and dark modes

**Don't:**

- Use hardcoded colors in SCSS
- Use `*ngIf` or `*ngFor` (use `@if`/`@for`)
- Skip the barrel import (`@use '../../../styles/index' as *`)
