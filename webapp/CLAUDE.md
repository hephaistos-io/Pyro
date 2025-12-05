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
├── components/        # Reusable UI components (navbar, hero, features, etc.)
├── pages/             # Route-level page components (home, pricing)
├── services/          # Singleton services (providedIn: 'root')
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

Use `:host-context(.dark-mode)` for component-scoped dark mode styles:

```scss
.element {
  background: white;
  color: $charcoal;

  :host-context(.dark-mode) & {
    background: $dark-card;
    color: $dark-text;
  }
}
```

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

- Use BEM-like naming: `.component`, `.component-element`, `.component-element-modifier`
- All colors, fonts, and spacing should use variables from `_variables.scss`
- Component styles are scoped (no global classes except from `src/styles/`)
- Keep nesting shallow (max 3 levels)

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
import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { PricingComponent } from './pages/pricing/pricing.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'pricing', component: PricingComponent },
  { path: '**', redirectTo: '' }  // Wildcard redirects to home
];
```

**Routing Patterns:**

- Page components map directly to routes
- Keep route configuration simple

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
