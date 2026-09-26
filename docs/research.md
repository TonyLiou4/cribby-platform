# Cribby product research

Research completed before the web implementation on September 25, 2026.

## Product evidence from Project Update 1

Cribby serves renters who may need housing, roommates, or both, and property managers who need a direct way to reach renter groups. The key product insight is that a roommate group - not always the individual renter - is the correct unit for shared-housing discovery.

The existing repositories already provide authentication, renter and manager profiles, property search, favorites, listing management, messaging, verification, reporting, and moderation. Roommate matching and group formation remain the largest backend gaps.

## Competitive patterns

- Roommates.com uses a short profile -> preferences -> matches -> messaging loop and emphasizes identity verification: https://www.roommates.com/about-us
- SpareRoom combines detailed room or roommate posts, filters, direct messaging, and in-person viewing: https://www.spareroom.com/content/about-us/how-spareroom-works
- Zillow's renter hub centralizes search, saved properties, applications, and status tracking: https://www.zillow.com/rent/renter-search-center/
- Apartments.com makes favorites a first-class renter tool: https://renterhelp.apartments.com/article/250-how-do-i-keep-track-of-listings-that-interest-me

Cribby's defensible wedge is not another generic property feed. It is the shared group profile: combined budget, timing, location, and bedroom needs remain synchronized from roommate discovery through landlord contact.

## Trust and compliance

- HUD identifies race, color, national origin, religion, sex, familial status, and disability as protected characteristics in housing: https://www.hud.gov/helping-americans/fair-housing-act-overview
- HUD's digital-advertising guidance warns against targeting or content that creates discriminatory housing preferences: https://www.hud.gov/sites/dfiles/FHEO/documents/FHEO_Guidance_on_Advertising_through_Digital_Platforms.pdf
- The FTC recommends verifying the property or manager, viewing the property before payment when possible, and rejecting wire, gift-card, or cryptocurrency payment demands: https://consumer.ftc.gov/articles/rental-listing-scams

The MVP therefore uses transparent preference-based explanations, verification status, reporting, and moderated in-app messaging. It should not rank housing or expose landlord filters based on protected characteristics.

## MVP decisions

- Open on the working dashboard, not a marketing hero.
- Keep student and property-manager tools in one product with an explicit workspace switch.
- Make group readiness and group-ranked homes the student's primary dashboard.
- Let managers see inquiry quality and respond quickly without exposing sensitive screening data.
- Keep authentication and persistence in the existing Spring API; use the web prototype to validate information architecture and conversion before funding infrastructure changes.
