# Cribby

Cribby is a student-housing marketplace that connects three steps normally split across separate products: finding compatible roommates, forming a household, and finding a home the group can afford together.

## Monorepo

- `apps/web` - responsive web workspace for students and property managers
- `apps/mobile` - existing Expo mobile application, preserved as a starting point
- `services/api` - existing Spring Boot API with authentication, listings, favorites, messages, verification, and moderation
- `docs/research.md` - product and market research that shaped the MVP

## Run the website

```bash
cd apps/web
pnpm install
pnpm dev
```

## MVP focus

The web product optimizes for one measurable loop:

1. A student completes housing and lifestyle preferences.
2. The student connects with compatible roommates.
3. The group agrees on a combined budget and move-in date.
4. Cribby ranks homes for the group rather than for one person.
5. The group messages a verified property manager and schedules a tour.

## Hypothetical $100 runway plan

No money was spent or authorized while building this project. If the team later chooses to use the theoretical $100 constraint, preserve the full amount for validation after the core flow is usable:

- $40 - four $10 student interview incentives
- $30 - campus launch materials and QR-code flyers
- $20 - direct property-manager outreach and listing verification costs
- $10 - contingency for a domain or an additional interview incentive

Do not buy ads before at least five students and two property managers complete the prototype flow.
