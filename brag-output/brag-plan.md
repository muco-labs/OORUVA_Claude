# Brag Plan: OORUVA

## What is this app?
An Android app that finds the street vendors around you — chai stalls, samosa carts,
juice corners, the mobile-repair guy — and, given how many of you there are and what
you've got to spend, tells you which one fits.

## Step 1 rubric answers

1. **What is the app?** A Material 3 Android app (Kotlin + Jetpack Compose) for
   discovering street vendors nearby, checking in, earning points, and finding a
   stall that fits a group's headcount and budget.
2. **Funniest / most impressive claim.** The Group Finder: `"How many people?"` +
   `"Total Budget (₹)"` → `"Results (5 people, ₹1500 budget)"` → `"₹300 per person ·
   Perfect fit"`. A budget calculator for street food is a genuinely good idea nobody
   builds.
3. **Visual hook.** The vendor card: a giant single-letter avatar block in
   `#6200EE` at 30% alpha, a gold `#FFC107` star row, and `"2.3 km away"` in bold purple.
4. **What to show from the actual UI.** The home feed (Chai Wali ★4.5 · 48 reviews,
   Street Samosa ★4.8 · 62, Fresh Juice Corner ★4.7 · 92) and the Group Finder
   result cards.
5. **Shortest satisfying video.** ~21s. The group/budget beat needs room to land —
   it's the reason to keep watching.
6. **Tone.** Preset `app-store`; direction: warm neighbourhood product film with
   Material 3 polish. The product is sincere, so the video is sincere.
7. **Audio.** Warm bed + motion-matched taps and card arrivals. Not a hype edit.
8. **Share caption.** Drafted below.
9. **User flow worth showing.** Entry → key action → result, twice over:
   search the feed → open a stall → check in; and set 5 people / ₹1500 →
   tap *Find Places for My Group* → three fitting stalls at ₹300 each.

## The angle
Everyone already knows the good chai stall near their office. Nobody knows the one
near yours. OORUVA is that knowledge, mapped — and the video's job is to make a
₹300-per-person samosa run look like the most useful software decision of the day.
No "streamline your discovery journey." Rupees, stars, and metres.

## Hook (first 2-3 seconds)
Black. Three lines slam in on the beat, one at a time:
**5 people.** → **₹1500.** → **Where do you eat?**
Then the question holds alone for a beat. That's the whole pitch in six words.

## Key moments (the middle)
- The purple `#6200EE` header bar wiping across and the wordmark **OORUVA** landing
  over `Discover Street Vendors Around You`.
- The home feed filling in card by card — the big letter avatars (C, S, F), the gold
  star rows, `4.8 (62 reviews)`, `2.3 km away`.
- The Group Finder doing arithmetic in public: the people counter ticking 1→5 under
  a tapping cursor, `1500` typing into the ₹ field, then three result cards landing —
  **Street Samosa / Chai Wali / Juice Corner**, each stamped `₹300 per person · Perfect fit`.
- The check-in: pin press on Chai Wali, points counter rolling up to **2,450**.

## Outro / punchline
Points counter settles on `2,450`. Cut to the wordmark on the purple field with the
line: **Your street. Already reviewed.** Hold, fade.

## User flow worth showing
1. **Entry** — home feed, search field, vendor cards with ratings and distance.
2. **Key action** — Group Finder: set headcount, set budget, tap *Find Places for My Group*.
3. **Result** — three ranked stalls with per-person cost; then a check-in that pays out points.

## Tone
- Preset: `app-store`
- Creative direction: warm neighbourhood product film — chai-stall energy, Material 3 restraint
- Interpretation: clean card reveals, smooth slides, no flash cuts or shake. Confidence
  comes from the numbers being specific, not from the edit shouting. Every scene shows
  the app doing something a person would actually do.

## Format: landscape — 1920x1080
## Duration: 21.6s target

## Visual identity (from the project)
- Background: `#FFFBFE` (light surface), `#1C1B1F` (dark scenes / hook)
- Accent: `#6200EE` (primary), `#3700B3` (primary dark), `#03DAC6` (secondary)
- Text: `#1C1B1F` on light, `#E7E1E6` on dark, `#FFFFFF` on the purple header
- Rating gold: `#FFC107`
- Display font: Roboto Bold (Compose `FontFamily.Default`, `titleLarge` 22sp Bold)
- Body font: Roboto Regular (`bodyLarge` 16sp / 24sp line height)
- Strongest visual element: the vendor card — letter-avatar block, gold star row,
  `2.3 km away` in bold primary

## Share copy (draft)
Built OORUVA: tell it there are 5 of you with ₹1500 and it finds the street food
stall that actually fits. ₹300 each, ★4.8, 2.3 km away. ☕🥟

## Audio direction
- Role: warm bed with sparse, motion-matched accents
- Music: `happy-beats-business-moves-vol-9-by-ende-dot-app.mp3` (114.84 BPM, upbeat but not frantic)
- Music treatment: start at 0.00s, duck slightly under the hook lines, full presence from
  the wordmark reveal, fade out across the final 1.2s of the outro
- Music cue guidance: preset cue file read. Target strong cues at **3.70s** (wordmark slam),
  **6.34s** (feed scene opens), **11.60s** (cut to Group Finder). Beat grid for sequential
  reveals: vendor cards at 7.40 / 8.44 / 9.50; result cards at 14.76 / 15.81 / 16.86 —
  every other beat, never the 0.53s grid, so each line stays readable.
- Audio-reactive treatment: subtle — let the purple header and the letter-avatar blocks
  breathe slightly with the bass. No waveform bars, no pulsing type.
- SFX posture: moderate, motion-matched — soft taps on the counter and button press,
  light key ticks on the typed `1500`, one clean arrival tick per card, one warmer
  confirmation on the check-in
- Audio-coupled moments: counter ticks 1→5, typed budget field, three-card arrivals
  (twice), points count-up to 2,450
- Restraint rule: no whoosh on every cut, no riser into the outro, no impact stinger on
  the logo. If a sound isn't matched to something moving on screen, it doesn't go in.

## Storyboard

### Scene 1 — Hook — 3.70s
Near-black `#1C1B1F` field. Three lines arrive one at a time, large Roboto Bold, white:
`5 people.` (0.53s), `₹1500.` (1.60s), `Where do you eat?` (2.65s). First two lines stay
on screen; the question holds alone for the last beat as the first two fade to 30%.
Sequential/interaction: yes — three lines, ~1.05s apart, each held at least 0.8s; the
question line holds ~1.05s fully settled.
Audio intent: sparse and confident, bed only, slight duck under each line.
Audio-coupled idea: one soft accent per line arrival, aligned to beats 0.53 / 1.59 / 2.65.
Music: warm upbeat bed, low presence.
Transition mood: clean → Scene 2

### Scene 2 — Reveal — 2.64s
Purple `#6200EE` bar wipes left→right across the frame; **OORUVA** lands on the strong cue
at 3.70s in white Roboto Bold, with `Discover Street Vendors Around You` fading in beneath
at 80% white. Holds ~1.4s.
Sequential/interaction: none — single slam, then hold.
Audio intent: the bed opens up to full presence here; this is the lift.
Audio-coupled idea: one clean arrival accent on the wordmark at 3.70s. No stinger.
Music: full presence from here.
Transition mood: smooth wipe → Scene 3

### Scene 3 — The feed — 5.26s
Phone-shaped frame on `#FFFBFE`. Purple header (`OORUVA` / `Discover Street Vendors Around
You`), search row below it. Three vendor cards arrive bottom-to-top on beats 7.40 / 8.44 /
9.50: **Chai Wali** `FOOD` ★4.5 `(48 reviews)` `2.3 km away`; **Street Samosa** `FOOD`
★4.8 `(62 reviews)`; **Fresh Juice Corner** `FOOD` ★4.7 `(92 reviews)`. Each card carries
the letter-avatar block (C / S / F) in primary at 30% alpha and a gold star row. Full set
holds ~2.1s.
Sequential/interaction: yes — three cards one by one, ~1.05s apart, then the whole set holds.
Audio intent: satisfying, tactile, unhurried — the app filling with real places.
Audio-coupled idea: one light arrival tick per card; a gold shimmer is optional but must
stay under the music.
Transition mood: slide → Scene 4

### Scene 4 — Group Finder — 6.31s
`Find places for your group & budget`. A cursor taps `+` and the counter ticks
**1 → 5** (four taps on beats, 11.60–13.18). `1500` types into the `Total Budget (₹)` field
with light key ticks. Cursor presses **Find Places for My Group** at 14.22. Result cards land
at 14.76 / 15.81 / 16.86: **Street Samosa**, **Chai Wali**, **Juice Corner** — each showing
`₹300/person`, `4.5 ★ • Perfect fit`. Full set holds ~1.05s under the header
`Results (5 people, ₹1500 budget)`.
Sequential/interaction: yes — simulated counter taps and typing, then three result cards
every other beat, each held to the readable floor plus a full-set hold.
Audio intent: the payoff — the arithmetic landing, cleanly and on the beat.
Audio-coupled idea: counter tick per tap, key ticks on the typed budget, button press,
one arrival tick per result card.
Transition mood: smooth wipe → Scene 5

### Scene 5 — Check-in and outro — 3.68s
Business detail for **Chai Wali**: `Hours 06:00-22:00`, `Main Street`. Cursor presses
**Check-in Here**; a pin marker plants. Cut to `Your OORUVA Points` counting up to **2,450**
in primary. Purple field takes the frame, **OORUVA** wordmark centred with
`Your street. Already reviewed.` Music fades across the last 1.2s.
Sequential/interaction: yes — simulated button press, then a count-up.
Audio intent: warm confirmation, then release. No triumphal stinger.
Audio-coupled idea: soft confirmation on the check-in press; gentle ticks under the
points count-up, thinning as the music fades.
Transition mood: soft fade → end

**Music mood for this video:** upbeat
**Audio summary:** A warm 115 BPM bed that starts low under the hook, opens at the wordmark,
carries tactile motion-matched ticks through the feed and the budget maths, and fades out
under the final count-up — the sound of an app being used, not an ad being shouted.

---

## Timing check
3.70 + 2.64 + 5.26 + 6.31 + 3.68 = **21.59s** — inside the 15–25s law, in the 18–22s sweet spot.

## Source material used (all real, from this repo)
- `app/src/main/kotlin/com/ooruva/app/ui/screens/HomeScreen.kt` — vendor names, ratings,
  review counts, `2.3 km away`, letter-avatar card
- `.../GroupFinderScreen.kt` — `How many people?`, `Total Budget (₹)`,
  `Find Places for My Group`, `Results (N people, ₹B budget)`, `Perfect fit`, `₹X/person`
- `.../RewardsScreen.kt` — `Your OORUVA Points`, `2,450`
- `.../BusinessDetailScreen.kt` — `Check-in Here`, hours/address rows
- `.../ui/theme/Color.kt` — `#6200EE`, `#3700B3`, `#03DAC6`, `#FFFBFE`, `#1C1B1F`
- `app/src/main/res/values/strings.xml` — `OORUVA`, `Discover Street Vendors Around You`
