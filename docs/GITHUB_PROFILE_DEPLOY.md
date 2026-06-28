# GitHub profile deployment guide

Everything created locally to improve [github.com/kanav22](https://github.com/kanav22). Follow these steps to push changes live.

---

## What was created / updated in this workspace

| File | Purpose |
|------|---------|
| `README.md` | Polished hero README with badges, Mermaid diagram, quick start |
| `CONTRIBUTING.md` | Contributor guidelines |
| `docs/LINKEDIN_POST.md` | 4 copy-paste LinkedIn posts |
| `.github/ISSUE_TEMPLATE/*` | Bug + feature request templates |
| `.github/pull_request_template.md` | PR checklist |
| `profile-updates/kanav22/README.md` | Revised profile README (shorter top, collapsible sections) |
| `scripts/setup-github-profile.sh` | Batch-set repo topics and descriptions via `gh` |

---

## Step 1: Push KMP repo improvements

From this repo (`kmp-user-management`):

```bash
git add README.md CONTRIBUTING.md docs/LINKEDIN_POST.md .github/
git commit -m "Polish README and add GitHub community health files"
git push origin main
```

---

## Step 2: Update profile README

Copy the revised profile README to your profile repo:

```bash
# Clone if needed
git clone https://github.com/kanav22/kanav22.git ~/kanav22-profile
cp profile-updates/kanav22/README.md ~/kanav22-profile/README.md
cd ~/kanav22-profile
git add README.md
git commit -m "Restructure profile README for faster recruiter scan"
git push origin main
```

---

## Step 3: Set repo topics (automated)

Requires [GitHub CLI](https://cli.github.com/) authenticated as `kanav22`:

```bash
chmod +x scripts/setup-github-profile.sh
./scripts/setup-github-profile.sh
```

Dry run first:

```bash
./scripts/setup-github-profile.sh --dry-run
```

---

## Step 4: Pin 6 repos (manual — 2 minutes)

1. Go to [github.com/kanav22?tab=repositories](https://github.com/kanav22?tab=repositories)
2. Click **Customize your pins**
3. Pin in this order:
   1. `android-platform-starter`
   2. `kmp-user-management`
   3. `compose-commerce-catalog`
   4. `compose-movies-finder`
   5. `ai-on-device-android`
   6. `compose-golden-toolkit`

---

## Step 5: GitHub settings (manual — 1 minute)

- [ ] **Profile → Include private contributions** (if you contribute at work on private repos)
- [ ] **Profile photo** — professional headshot
- [ ] **Follow** orgs: `@android`, `@JetBrains`, `@cashapp`, `@google`
- [ ] **Follow** 20–30 Android/KMP engineers you respect

---

## Step 6: Publish content (biggest visibility lever)

| Asset | Location | Action |
|-------|----------|--------|
| LinkedIn posts | `docs/LINKEDIN_POST.md` | Post 1 per week |
| Medium article (KMP) | [draft in kanav22 repo](https://github.com/kanav22/kanav22/blob/main/docs/publishing/kmp-mvi-offline-first-medium.md) | Publish to Medium |
| Medium article (perf) | [draft in kanav22 repo](https://github.com/kanav22/kanav22/blob/main/docs/publishing/macrobenchmark-performance-budgets-medium.md) | Publish to Medium |
| Video walkthrough | [script in kanav22 repo](https://github.com/kanav22/kanav22/blob/main/docs/VIDEO-WALKTHROUGH-SCRIPT.md) | Record + YouTube |

---

## Step 7: Screenshot placeholders

The KMP README references architecture diagrams (Mermaid renders on GitHub). For extra impact, add app screenshots:

1. Run the app on Android emulator
2. Capture: user list, add user sheet, tablet detail pane
3. Save to `docs/images/` and add to README:

```markdown
![User list screen](docs/images/user-list.png)
```

---

## Optional: apply hero READMEs to other repos

The same pattern used for `kmp-user-management` can be applied to your other repos. Each needs:

- CI badge at top
- "Why this exists" section
- Architecture diagram (Mermaid)
- Quick start (5 commands max)
- Link back to [github.com/kanav22](https://github.com/kanav22)

Ask in Cursor to generate READMEs for `android-platform-starter`, `compose-movies-finder`, etc. if those repos are cloned locally.

---

## Verification checklist

After deploying, verify:

- [ ] [github.com/kanav22](https://github.com/kanav22) shows updated profile README
- [ ] 6 repos pinned
- [ ] Each repo has topics visible under "About"
- [ ] [kmp-user-management](https://github.com/kanav22/kmp-user-management) README renders Mermaid diagram
- [ ] CI badge shows green on KMP repo
