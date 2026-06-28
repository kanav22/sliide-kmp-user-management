# LinkedIn post drafts

Copy-paste ready posts to drive traffic to your GitHub profile and flagship repos.

---

## Post 1: android-platform-starter launch

**Hook:** Platform engineering is underrated on mobile.

I've open-sourced **android-platform-starter** — a production Android platform template I wish existed when I was bootstrapping teams at Paytm and Angel One.

What's inside:
→ Multi-module Compose architecture with clear boundaries
→ Hilt DI, Detekt, and Macrobenchmark wired from day one
→ ADRs for decisions that survive team turnover
→ GitHub Actions CI that catches regressions before merge

This isn't a toy app. It's the scaffolding I use when I need **40% faster deploys** and **2-week → 2-day release cycles**.

Code + architecture docs:
https://github.com/kanav22/android-platform-starter

If you're building a platform team or migrating a monolith, this might save you a quarter.

#AndroidDev #JetpackCompose #MobileEngineering #OpenSource

---

## Post 2: KMP + MVI + offline-first

**Hook:** Most KMP samples stop at "Hello World."

I built **kmp-user-management** to show what production KMP actually looks like:

→ MVI with explicit State / Intent / Effect
→ SQLDelight for offline-first reads
→ Ktor for network sync
→ Shared Compose UI on Android, iOS, and Desktop
→ CI on every push

I also wrote up the architecture decisions that survive production — not just the happy path:

https://github.com/kanav22/kmp-user-management/blob/main/docs/architecture/kmp-mvi-offline-first.md

Repo: https://github.com/kanav22/kmp-user-management

What's the hardest part of KMP you've hit in production? Curious what's blocking teams.

#KotlinMultiplatform #Compose #MobileArchitecture #OfflineFirst

---

## Post 3: OSS contributions (short)

Contributing upstream matters.

Two small PRs I'm proud of this month:
→ [Now in Android #2133](https://github.com/android/nowinandroid/pull/2133) — SyncInitializer docs fix
→ [SQLDelight #6287](https://github.com/sqldelight/sqldelight/pull/6287) — documentation clarity

Plus 6 CI-backed repos I'm maintaining:
https://github.com/kanav22

Open to Principal Android opportunities in London / remote UK.

#AndroidDev #OpenSource #Kotlin

---

## Post 4: Performance budgets (article promo)

When crash rates matter (2% → <0.1%), performance isn't optional.

I wrote about **performance budgets with Macrobenchmark and Baseline Profiles** — how to set measurable targets and enforce them in CI:

https://github.com/kanav22/android-platform-starter/blob/main/docs/articles/macrobenchmark-performance-budgets.md

Full template repo: https://github.com/kanav22/android-platform-starter

Performance is a feature. Treat it like one.

#AndroidDev #Performance #JetpackCompose

---

## Posting tips

1. Add a screenshot or architecture diagram as the post image (export from README Mermaid).
2. Post Tuesday–Thursday, 8–10am UK time for best reach.
3. Reply to every comment in the first 2 hours — algorithm boost.
4. Link to one repo per post; don't dump all 6 at once.
5. Pin your best-performing post to your LinkedIn profile.
