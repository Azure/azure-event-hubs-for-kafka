# GHAS Demo Files — Sargent & Lundy

These files demonstrate GitHub Advanced Security features. **Do NOT use any credentials in this folder in production.**

## Demo Scenarios

### 1. Push Protection Demo
**File:** `push-protection-test.sh`

Try pushing this branch with the fake secret in the file. GitHub will block the push with:
```
error GH009: Secrets detected! This push failed.
```

**Steps:**
1. Stage and commit the file: `git add . && git commit -m "add config"`
2. Push: `git push origin demo/ghas-security-features`
3. Observe push protection blocking the commit
4. Show the bypass workflow option

---

### 2. Secret Scanning Demo
**File:** `vulnerable-config.env`

This file contains test secrets that secret scanning will detect after they're in the repo:
- AWS-style access key
- Generic API token pattern

After the push (if bypassed or on a repo without push protection), go to:
**Security tab → Secret scanning alerts**

---

### 3. PR Annotation / Code Scanning Demo
**File:** `VulnerableController.cs`

This C# file has intentional vulnerabilities that CodeQL will detect:
- **SQL Injection** — string concatenation in a SQL query
- **Cross-Site Scripting (XSS)** — unsanitized user input in HTML response
- **Path Traversal** — unsanitized file path from user input

**Steps:**
1. Push this branch
2. Open a Pull Request to `main`
3. CodeQL runs via the workflow in `.github/workflows/codeql-analysis.yml`
4. Findings appear as **inline PR annotations** with Copilot Autofix suggestions

---

### 4. CodeQL Workflow
**File:** `.github/workflows/codeql-analysis.yml`

A minimal CodeQL workflow for C# that runs on PRs and pushes. This enables code scanning alerts and PR annotations.

---

## Cleanup
After the demo, delete this branch:
```bash
git branch -D demo/ghas-security-features
git push origin --delete demo/ghas-security-features
```
