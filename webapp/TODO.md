# Pyro Webapp - Technical Debt & Improvements

## Completed ✅

### Hardcoded Colors & Magic Color Values (Priority: Low, Effort: Very Low)

**Status:** ✅ Completed 2025-12-05

**What was done:**

- Added 3 new base color variables to `_variables.scss`: `$black`, `$white`, `$dark-bg-deeper`
- Replaced 16 hardcoded RGBA/hex color values across 9 files with variable-based equivalents
- All colors now use the pattern `rgba($variable, opacity)` for consistency
- Zero visual changes - purely a maintainability improvement

**Files modified:**

- `src/styles/_variables.scss`
- `src/styles.scss`
- `src/styles/_buttons.scss`
- `src/app/components/navbar/navbar.component.scss`
- `src/app/components/hero/hero.component.scss`
- `src/app/components/features/features.component.scss`
- `src/app/components/cta/cta.component.scss`
- `src/app/components/footer/footer.component.scss`
- `src/app/pages/pricing/pricing.component.scss`

---

### Complex SVG Management (Priority: Medium, Effort: Medium)

**Status:** ✅ Completed 2025-12-05

**What was done:**

- Modernized features component to use `inject(DomSanitizer)` instead of constructor injection
- Fixed navbar logo SVG to use `stroke="currentColor"` for theme awareness
- Added SVG color styling to navbar SCSS (white on red background)
- Removed deprecated `::ng-deep` selector from features component SCSS
- Maintained inline SVG approach for simplicity (no external dependencies)

**Files modified:**

- `src/app/components/features/features.component.ts` - Injection pattern
- `src/app/components/features/features.component.scss` - Removed ::ng-deep
- `src/app/components/navbar/navbar.component.html` - SVG stroke attribute
- `src/app/components/navbar/navbar.component.scss` - Added SVG color styling

---

## Remaining Misalignments

### 1. Missing Tests (Priority: HIGH, Effort: MEDIUM-HIGH)

**Issue:**
Zero test files exist in the codebase. The project is configured to use Vitest (as per CLAUDE.md), but no `.spec.ts` files have been created.

**Affected Components:**

- `app.component.ts` - Root component
- `navbar.component.ts` - Navigation
- `hero.component.ts` - Interactive state management, feature toggles
- `features.component.ts` - Data-driven SVG rendering
- `how-it-works.component.ts` - Data-driven rendering
- `footer.component.ts` - Static component
- `cta.component.ts` - Static component
- `pricing.component.ts` - Form validation logic (HIGH PRIORITY)
- `theme.service.ts` - Signal-based state management (HIGH PRIORITY)

**Recommended Priority Order:**

1. **High Priority:**
  - `pricing.component.ts` - Has form validation logic that needs testing
  - `theme.service.ts` - Signal-based state management affects entire app
  - `features.component.ts` - SVG rendering, data-driven logic

2. **Medium Priority:**
  - `hero.component.ts` - Interactive state, multiple feature toggles
  - `how-it-works.component.ts` - Data-driven rendering

3. **Low Priority:**
  - `navbar.component.ts`, `footer.component.ts`, `cta.component.ts` - Mostly static components

**Implementation Notes:**

- Total lines of code: ~1,411 lines across 30 files
- Need to set up Vitest test infrastructure if not already configured
- Follow Angular testing best practices with standalone components
- Test both light and dark mode for components with theme awareness

**Relevant Files:**

- Create: `src/app/**/*.spec.ts` files for all components and services
- Reference: CLAUDE.md mentions Vitest as the testing framework

---

### 2. TODO Comments (Priority: LOW, Effort: LOW-MEDIUM)

**Issue:**
Only 1 TODO comment exists in the codebase, indicating incomplete functionality.

**Location:** `src/app/pages/pricing/pricing.component.ts:27`

**Code:**

```typescript
onSubmit()
:
void {
  if(this.validateEmail(this.email)
)
{
  // TODO: Integrate with actual email service
  console.log('Email submitted:', this.email);
  this.submitted = true;
  this.error = '';
}
else
{
  this.error = 'Please enter a valid email address';
}
}
```

**Current Behavior:**

- Form validates email with regex: `/^[^\s@]+@[^\s@]+\.[^\s@]+$/`
- On submission, only logs to console
- Shows success message but doesn't actually submit to backend

**Implementation Requirements:**

1. **Backend Integration:**
  - Determine email service provider (SendGrid, Mailchimp, custom API, etc.)
  - Add HTTP service for API calls
  - Handle API errors and loading states

2. **UI Enhancements:**
  - Add loading spinner during submission
  - Handle network errors gracefully
  - Provide user feedback for various error states

3. **Service Layer:**
  - Create an email/signup service (`ng generate service services/signup`)
  - Use modern `inject(HttpClient)` pattern
  - Add proper error handling and retry logic

**Related Files:**

- `src/app/pages/pricing/pricing.component.ts` - Form logic
- `src/app/pages/pricing/pricing.component.html` - Form UI
- To create: `src/app/services/signup.service.ts` - Email submission logic

**Considerations:**

- This is likely blocked until backend API is ready
- May want to add environment configuration for API endpoints
- Consider adding email validation on backend as well
- GDPR/privacy compliance for email collection

---

## Project Guidelines Reference

From CLAUDE.md:

- ✅ "Don't use hardcoded colors - use variables from _variables.scss" - **COMPLETED**
- ✅ Use modern `inject()` function instead of constructor injection - **COMPLETED**
- ✅ Avoid `@import` in SCSS - use `@use/@forward` only
- ✅ TypeScript strict mode enabled - all code must be strictly typed
- ✅ Test both light and dark modes for all components
- ✅ Prefer readability and simplicity over cleverness
- ✅ Angular 21 standalone components architecture

---

## Long-term Improvements

1. **Add comprehensive test coverage** - Should be prioritized as it's marked HIGH priority
2. **Decide on unified SVG management strategy** - Discuss with team
3. **Email service integration** - Blocked on backend/infrastructure decisions
