# Katari Extensions

Public extensions for [Katari](https://github.com/katariapp/katari).

## Add the repository

Add this URL as an extension repository in Katari:

```text
https://raw.githubusercontent.com/katariapp/katari-extensions/repo/index.json
```

## Extension development with OpenCode

This repository includes an OpenCode command and skill for evidence-based extension development. Open this repository in OpenCode and run:

```text
/develop-extension https://example.com
```

The command first investigates the website and inspects the configured Entry SDK tag from Katari's public GitHub repository without modifying files or requiring a local Katari checkout. After reviewing its capability report, explicitly approve implementation in a later turn. AI-assisted changes remain subject to the same review, validation, licensing, and removal policies as any other contribution.

See [CONTRIBUTING.md](CONTRIBUTING.md) for the complete workflow.

## Disclaimer

This project is not affiliated with the websites supported by its extensions, Mihon, Tachiyomi, or Keiyoushi. It distributes client integration code and does not host media from supported websites. Do not request Katari or extension support from unrelated projects.

See [LEGAL.md](LEGAL.md) for site-owner and legal removal requests.
