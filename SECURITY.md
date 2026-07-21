# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 4.x     | :white_check_mark: |
| < 4.0   | :x:                |

## Reporting a Vulnerability

If you believe you have found a **real** security vulnerability in RxJavaExtensions, please report it privately via [GitHub Security Advisories](https://github.com/akarnokd/RxJavaExtensions/security/advisories/new).

**Do not open a public issue.**

### What is actually in scope

RxJavaExtensions is a pure operator / utility library. It contains:

- No network code
- No serialization / deserialization
- No native code
- No file-system or process interaction
- No privileged operations
- No authentication, authorization, or cryptographic primitives

Therefore the only issues that will ever be considered security vulnerabilities are those that allow **remote code execution, privilege escalation, or unintended data exfiltration** when the library is used as documented.

The following are **explicitly out of scope** (and will be closed immediately):

- Resource exhaustion / OutOfMemoryError caused by a malicious or buggy upstream
- Violations of the Reactive Streams protocol by the caller
- “Theoretical” issues that require the application developer to already be doing something insane
- Style nits, missing null-checks, or “this could be DoS’d if you pass a hostile Observable”
- Anything an LLM hallucinated after a 30-second glance at the repo

### A note to low-effort researchers

If you contacted the maintainer *before* even skimming the README or source tree to notice that this library has essentially zero classic attack surface, please go back and do that first. It will save everyone time.

We are happy to look at genuine findings. We are not interested in AI-generated noise or CVSS-inflated correctness bugs.

## Disclosure Policy

- We aim to acknowledge reports within 72 hours.
- We will keep you informed of progress.
- CVEs (if any) will only be requested **after** a fix is released.
- We follow coordinated disclosure. Please give us at least 90 days before any public disclosure.

Thank you for helping keep the reactive ecosystem safe — the right way.

------

🪐 via Grok
