# Pyro Webapp - Project Guidelines

## Project Overview

**Pyro** is a feature flag management platform designed for engineers. This webapp serves as the frontend interface,
built with modern web technologies.

**Tech Stack:**

- Angular 21 (standalone components)
- SCSS for styling
- TypeScript with strict mode
- Vitest for testing

**Design Inspiration:** www.enode.com

**Philosophy:** When working on this project, we prefer **readability and simplicity over cleverness**. Always ask for
input before proceeding with big, impactful changes.

---

## Project Structure

The codebase follows a clean, organized structure that separates concerns:

```
src/app/
├── api/generated/     # Auto-generated API client (ng-openapi-gen)
├── components/        # Reusable UI components
│   ├── navbar/
│   ├── footer/
│   ├── hero/
│   ├── features/
│   ├── how-it-works/
│   ├── cta/
│   ├── app-card/                    # Signal-based card component
│   ├── onboarding-overlay/          # First-time user onboarding
│   ├── company-creation-form/       # Company creation form
│   └── application-creation-form/   # Application creation form
├── guards/            # Route guards (authGuard)
├── interceptors/      # HTTP interceptors (authInterceptor)
├── layouts/           # Page layout wrappers
│   └── dashboard-layout/
├── pages/             # Route-level page components
│   ├── home/
│   ├── pricing/
│   ├── login/         # Authentication page
│   ├── register/      # Registration with password strength
│   └── dashboard/     # Protected dashboard (requires auth)
├── services/          # Singleton services (providedIn: 'root')
├── utils/             # Utility functions (error-handler)
├── app.component.ts   # Root component
├── app.routes.ts      # Route definitions
└── app.config.ts      # Application configuration

src/styles/            # Global SCSS system
├── _index.scss        # Barrel file that re-exports all modules
├── _variables.scss    # Color palette, dark mode variables
├── _mixins.scss       # Reusable SCSS mixins
├── _typography.scss   # Font families, heading styles
├── _animations.scss   # Keyframe animations
└── _buttons.scss      # Button component styles
```

**Key Principles:**

- **Components** are reusable UI elements (navbar, hero, features)
- **Pages** are route-level components that compose multiple components
- **Services** are singleton instances for shared state/logic
- **Standalone architecture**: No NgModules, everything is standalone

---

## Component Conventions

Every component follows a consistent structure with three files:

### File Structure

```
component-name/
├── component-name.component.ts       # TypeScript logic
├── component-name.component.html     # Template
└── component-name.component.scss     # Styles
```

### Basic Component Structure

```typescript
import { Component } from '@angular/core';

@Component({
  selector: 'app-component-name',
  standalone: true,
  imports: [],
  templateUrl: './component-name.component.html',
  styleUrl: './component-name.component.scss'  // Note: styleUrl (singular)
})
export class ComponentNameComponent {
  // Component logic
}
```

### Service Injection Pattern

Use the modern `inject()` function instead of constructor injection:

```typescript
import { Component, inject } from '@angular/core';
import { ThemeService } from '../../services/theme.service';

@Component({
  selector: 'app-example',
  standalone: true,
  // ...
})
export class ExampleComponent {
  private themeService = inject(ThemeService);

  someMethod(): void {
    this.themeService.toggleTheme(true);
  }
}
```

**Component Naming:**

- Selector: `app-{component-name}` (kebab-case with `app-` prefix)
- Class: `{ComponentName}Component` (PascalCase)
- Files: `{component-name}.component.{ts,html,scss}` (kebab-case)

### Creating Reusable Components

When building UI elements that will be used in multiple places or have distinct visual patterns, **create them as reusable components from the start**. Don't inline styles in page components and refactor later.

**When to create a reusable component:**

- UI element appears (or will appear) more than once
- Element has its own distinct styling and behavior (cards, buttons, badges, etc.)
- Element could logically be used elsewhere in the app

**Example - Reusable Card Component:**

```typescript
// app-card.component.ts
import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-card',
  standalone: true,
  templateUrl: './app-card.component.html',
  styleUrl: './app-card.component.scss'
})
export class AppCardComponent {
  name = input<string>();           // Signal-based input
  isAddCard = input(false);         // Input with default value
  cardClick = output<void>();       // Output event

  onClick(): void {
    this.cardClick.emit();
  }
}
```

```html
<!-- app-card.component.html -->
<button class="app-card" [class.app-card--add]="isAddCard()" (click)="onClick()">
  @if (isAddCard()) {
    <span class="app-card__icon">+</span>
    <span class="app-card__label">New Item</span>
  } @else {
    <span class="app-card__name">{{ name() }}</span>
  }
</button>
```

```scss
// app-card.component.scss - uses BEM naming
@use '../../../styles/index' as *;

.app-card {
  display: flex;
  align-items: center;
  padding: 1rem 1.5rem;
  background: $white;
  border: 1px solid $dark-border;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;

  @include dark-mode {
    background: $dark-card;
  }

  &__name { /* element */ }
  &__icon { /* element */ }
  &--add { /* modifier */ }
}
```

**Usage in parent component:**

```html
<app-card name="My Item" (cardClick)="onItemClick()" />
<app-card [isAddCard]="true" (cardClick)="onAddClick()" />
```

---

## Angular Patterns

### Signal-Based State Management

For simple state, use Angular signals instead of RxJS observables:

```typescript
import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  isDarkMode = signal(false);

  toggleTheme(isDark: boolean): void {
    this.isDarkMode.set(isDark);
    // Update DOM
    if (isDark) {
      document.body.classList.add('dark-mode');
    } else {
      document.body.classList.remove('dark-mode');
    }
  }
}
```

### Modern Template Syntax

Use the new control flow syntax (not `*ngIf` or `*ngFor`):

```html
<!-- Conditionals -->
@if (condition) {
  <div>Shown when condition is true</div>
} @else {
  <div>Shown when condition is false</div>
}

<!-- Loops -->
@for (item of items; track item.id) {
  <div>{{ item.name }}</div>
}
```

**Key Patterns:**

- Always use `track` in `@for` loops for performance
- Use signals for reactive state when appropriate
- TypeScript strict mode is enabled - all code must be strictly typed
- Define interfaces for data structures

---

## SCSS Guidelines

### Import Pattern

All components should import from the centralized barrel file:

```scss
@use '../../../styles/index' as *;

.component {
  background: $pyro-red;
  color: white;
  padding: 2rem;
}
```

**Important:**

- ✅ **Always use** `@use` and `@forward` (modern Sass)
- ❌ **Never use** `@import` (deprecated, will be removed in Dart Sass 3.0)
- The `as *` syntax imports without namespace prefix for simplicity

### Dark Mode Pattern

Use the `dark-mode` mixin for component-scoped dark mode styles:

```scss
.element {
  background: $white;
  color: $charcoal;

  @include dark-mode {
    background: $dark-card;
    color: $dark-text;
  }
}
```

**Important:** Always use `@include dark-mode` instead of writing `:host-context(.dark-mode) &` directly. The mixin is defined in `_mixins.scss` and ensures consistency.

### Responsive Breakpoints

Use consistent breakpoints across the project:

```scss
// Mobile
@media (max-width: 768px) {
  .component {
    padding: 2rem 1rem;
  }
}

// Tablet and smaller desktops
@media (max-width: 1205px) {
  .component {
    flex-direction: column;
  }
}

// Larger layouts
@media (max-width: 1205px) {
  .component {
    max-width: 100%;
  }
}
```

**SCSS Conventions:**

- Use BEM naming for component styles: `.component`, `.component__element`, `.component--modifier`
- All colors, fonts, and spacing should use variables from `_variables.scss`
- Component styles are scoped (no global classes except from `src/styles/`)
- Keep nesting shallow (max 3 levels)

### Using Style Variables

Always use defined variables instead of hardcoded values:

```scss
// ✅ Good - uses variables
.card {
  background: $white;
  border: 1px solid $dark-border;
  color: $charcoal;
  font-family: $font-sans;

  @include dark-mode {
    background: $dark-card;
    color: $dark-text;
  }
}

// ❌ Bad - hardcoded values
.card {
  background: white;
  border: 1px solid rgba(white, 0.1);
  color: #2D2D2D;
  font-family: 'DM Sans', sans-serif;
}
```

**Available variables to use:**

- Colors: `$white`, `$black`, `$charcoal`, `$charcoal-light`, `$off-white`, `$warm-gray`
- Brand: `$pyro-red`, `$pyro-red-light`, `$pyro-red-pale`, `$pyro-red-deep`
- Dark mode: `$dark-bg`, `$dark-card`, `$dark-border`, `$dark-text`, `$dark-text-muted`
- Fonts: `$font-serif` (headings), `$font-sans` (body/UI)

---

## Design System

### Color Palette

```scss
// Primary Colors
$pyro-red: #C94C4C;
$pyro-red-light: #E8A5A5;
$pyro-red-pale: #FDF5F5;
$pyro-red-deep: #8B3A3A;

// Neutrals
$charcoal: #2D2D2D;
$charcoal-light: #4A4A4A;
$off-white: #FAFAFA;
$warm-gray: #F5F3F1;

// Dark Mode
$dark-bg: #1a1a1a;
$dark-bg-deeper: #141414;  // For footer/deeper backgrounds
$dark-card: #252525;
$dark-border: #3a3a3a;
$dark-text: #f5f5f5;
$dark-text-muted: #a0a0a0;
```

### Typography

```scss
// Font Families
$font-serif: 'Fraunces', serif;    // Use for headings
$font-sans: 'DM Sans', sans-serif; // Use for body text and UI

// Headings use clamp() for responsive sizing
// h1: clamp(2.5rem, 5vw, 3.75rem)
// h2: clamp(2rem, 4vw, 2.75rem)
```

### Button Styles

Use predefined button classes:

```html
<button class="btn-primary">Primary Action</button>
<button class="btn-secondary">Secondary Action</button>
```

**Design Principles:**

- Subtle, performant animations
- Generous whitespace
- Consistent spacing scale
- Accessible color contrast

---

## Code Quality Standards

### TypeScript Configuration

- **Strict mode enabled**: All strict compiler options are on
- **Explicit typing**: Avoid `any`, always define types
- **Interface definitions**: Define interfaces for all data structures

```typescript
// Always define interfaces for data structures
interface Feature {
  icon: SafeHtml;
  title: string;
  description: string;
}

// Explicit return types for public methods
toggleTheme(isDark: boolean): void {
  this.isDarkMode.set(isDark);
}
```

### Naming Conventions

- **Variables & Functions**: camelCase (`isDarkMode`, `toggleTheme`)
- **Classes & Interfaces**: PascalCase (`ThemeService`, `Feature`)
- **Files**: kebab-case (`theme.service.ts`, `hero.component.ts`)
- **Component Selectors**: kebab-case with `app-` prefix (`app-navbar`)
- **SCSS Classes**: kebab-case, BEM-like (`.nav-links`, `.feature-card-icon`)

### Prettier Configuration

The project uses Prettier with these settings:

- Print width: 100 characters
- Quotes: Single quotes
- Angular parser for HTML files

---

## Development Workflow

### Common Commands

```bash
# Development Server
ng serve                    # Start dev server at localhost:4200
npm start                   # Same as ng serve

# Building
ng build                    # Production build
ng build --watch            # Watch mode for development

# Testing
ng test                     # Run tests with Vitest

# Code Generation
ng generate component components/my-component  # Generate new component
ng generate service services/my-service        # Generate new service
```

### Package Manager

This project uses **npm 11.6.4** as specified in `package.json`. Use npm for all package operations.

### Git Workflow

- Create feature branches for new work
- Write descriptive commit messages
- Ask before making large architectural changes

### Bundle Budgets

The project has bundle size budgets configured:

- Warning at 500kB
- Error at 1MB

Keep bundle sizes reasonable, especially for component styles.

---

## Routing Conventions

Routes are defined in `app.routes.ts`:

```typescript
export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'pricing', component: PricingComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'login', component: LoginComponent },
  {
    path: 'dashboard',
    component: DashboardLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', component: DashboardComponent }
    ]
  },
  { path: '**', redirectTo: '' }
];
```

**Routing Features:**

- **Anchor scrolling enabled** - `anchorScrolling: 'enabled'` for same-page navigation
- **Scroll restoration** - `scrollPositionRestoration: 'enabled'`
- **Auth guard** - Protects `/dashboard` route, redirects to `/login?returnUrl=...`
- **Child routes** - Dashboard uses `DashboardLayoutComponent` as wrapper

### Navigation Patterns

The project uses different navigation methods depending on the navigation type:

**Route Navigation (Different Pages):**
Use Angular's `routerLink` directive for navigating to different routes:

```html
<a routerLink="/pricing">Pricing</a>
<a [routerLink]="['/about']">About</a>
<a class="btn" routerLink="/pricing">Get Started</a>
```

**Same-Page Anchors (Section Scrolling):**
Use standard HTML anchor links for scrolling to sections on the current page:

```html
<a href="#features">Features</a>
<a href="#how-it-works">How It Works</a>
```

**Why This Mixed Approach?**

- `href="#section"` leverages native browser anchor functionality (bookmarkable, copy-paste URLs)
- `routerLink` provides Angular's routing features for page navigation
- Both are standards-compliant and appropriate for their use cases
- More performant (no router overhead for simple same-page scrolling)

---

## API Client (Generated)

### Overview

The API client is auto-generated from the backend OpenAPI spec using `ng-openapi-gen`.

**Location:** `src/app/api/generated/`

### Regenerating the Client

```bash
# 1. Start the backend and generate OpenAPI spec
./gradlew backend:webapp-api:generateOpenApiDocs

# 2. Generate TypeScript client from the spec
npx ng-openapi-gen
```

### Using the API

The generated `Api` service provides a **promise-based interface** (not RxJS):

```typescript
import { Api } from '../api/generated/api';

@Component({ ... })
export class MyComponent {
  private api = inject(Api);

  async loadData(): Promise<void> {
    const response = await this.api.getApplications();
    // response is typed based on OpenAPI spec
  }
}
```

### Available Endpoints

| Method                          | Endpoint                   | Description        |
|---------------------------------|----------------------------|--------------------|
| `register()`                    | POST /v1/auth/register     | User registration  |
| `authenticate()`                | POST /v1/auth/authenticate | User login         |
| `profile()`                     | GET /v1/user/profile       | Get current user   |
| `getCompanyForCurrentUser()`    | GET /v1/company            | Get user's company |
| `createCompanyForCurrentUser()` | POST /v1/company           | Create company     |
| `getApplications()`             | GET /v1/applications       | List applications  |
| `createApplication()`           | POST /v1/applications      | Create application |

---

## Authentication & Guards

### AuthService

Manages authentication state using signals:

```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
  isAuthenticated = signal(false);
  userEmail = signal<string | null>(null);

  login(token: string, email: string): void { ... }
  logout(): void { ... }
}
```

### AuthGuard

Protects routes requiring authentication:

```typescript
// guards/auth.guard.ts
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  // Redirect to login with return URL
  return router.createUrlTree(['/login'], {
    queryParams: { returnUrl: state.url }
  });
};
```

### AuthInterceptor

Automatically adds Bearer token to API requests:

```typescript
// interceptors/auth.interceptor.ts
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('auth_token');
  if (token) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }
  return next(req);
};
```

---

## Services Reference

### ThemeService

Manages dark/light mode with localStorage persistence.

### AuthService

Manages authentication state (isAuthenticated, userEmail, token storage).

### UserService

Manages user profile and company data with computed signals:

```typescript
@Injectable({ providedIn: 'root' })
export class UserService {
  user = signal<UserResponse | null>(null);
  company = signal<CompanyResponse | null>(null);

  // Computed signals for derived state
  hasCompany = computed(() => this.company() !== null);
  companyName = computed(() => this.company()?.name ?? '');
  greeting = computed(() => {
    const user = this.user();
    return user ? `Welcome, ${user.firstName}` : 'Welcome';
  });
}
```

---

## Modern Angular Patterns

### Converting Observables to Signals

Use `toSignal()` from `@angular/core/rxjs-interop`:

```typescript
import { toSignal } from '@angular/core/rxjs-interop';

@Component({ ... })
export class MyComponent {
  private route = inject(ActivatedRoute);

  // Convert observable to signal
  queryParams = toSignal(this.route.queryParams);
}
```

### Automatic Subscription Cleanup

Use `takeUntilDestroyed()` for RxJS subscriptions:

```typescript
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({ ... })
export class MyComponent {
  constructor() {
    someObservable$
      .pipe(takeUntilDestroyed())
      .subscribe(value => { ... });
  }
}
```

### Password Strength Validation

The register page uses `zxcvbn` for password strength:

```typescript
import zxcvbn from 'zxcvbn';

checkPasswordStrength(password: string): void {
  const result = zxcvbn(password);
  this.passwordStrength = result.score; // 0-4
  this.passwordFeedback = result.feedback.suggestions;
}
```

### Error Handling Utility

Centralized error handling in `utils/error-handler.util.ts`:

```typescript
export function handleApiError(error: unknown): string {
  // Extract user-friendly message from API errors
}
```

---

## Best Practices & Gotchas

### Do's

- ✅ Always ask before making big architectural changes
- ✅ Preserve the barrel pattern for SCSS imports (`@use '../../../styles/index' as *;`)
- ✅ Test both light and dark modes for all new components
- ✅ Keep components focused and simple - split large components
- ✅ Use `track` in all `@for` loops for performance
- ✅ Define interfaces close to where they're used (in component file if not reused)

### Don'ts

- ❌ Only use `@use/@forward` in SCSS
- ❌ Don't use hardcoded colors - use variables from `_variables.scss`
- ❌ Don't create "clever" solutions - prefer readable, simple code
- ❌ Don't skip testing both light and dark mode
- ❌ Don't use constructor injection - use `inject()` function

### Performance Tips

- Use `track` in `@for` loops to help Angular optimize rendering
- Use animation delays for staggered effects (see `hero.component.scss`)
- Keep bundle sizes small - check component SCSS file sizes

---

## Common Patterns Reference

Learn from existing code by examining these files:

**Data-Driven Components:**

- `src/app/components/features/features.component.ts` - Array of features rendered with `@for`

**Signal-Based Services:**

- `src/app/services/theme.service.ts` - Simple signal-based state management

**Modern Template Control Flow:**

- All component templates use `@if/@else` and `@for` syntax

**Component Composition:**

- `src/app/pages/home/home.component.ts` - Page component that imports and composes multiple feature components

**Form Handling:**

- `src/app/components/hero/hero.component.ts` - Interactive form with local state
- `src/app/pages/pricing/pricing.component.ts` - Email form with validation

**SCSS Patterns:**

- Any component SCSS file - See consistent import and dark mode patterns
