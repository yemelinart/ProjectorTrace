# Presentation website and PDF

The public presentation lives at https://yemelinart.github.io/ProjectorTrace/.

- `site/index.html`: English and Russian page content.
- `site/style.css`: responsive visual design.
- `site/app.js`: language switch, screenshot gallery, comparison slider and image enlargement.
- `site/assets/`: genuine screenshots from the original v8.5 APK running in an Android TV emulator. WebP files are compressed copies of the captures; the app interface and filter output are not mocked up.
- `site/features.json`: the menu-accessible feature inventory in English and Russian.
- `site/Projector-Trace-Presentation.pdf`: 12-page English presentation.

The website is static and has no build dependencies, analytics, accounts or tracking scripts. Its language preference is saved locally in the visitor's browser. GitHub Pages deploys `site/` when it changes on `main`.

## Screenshot provenance and artwork

Screenshots were captured at 1920 × 1080 on 2026-09-22 in a temporary, read-only Android TV emulator. A demonstration image and settings were loaded into the app to show its real menu, guides, filters and palette. The original APK and application source were not modified to create the presentation.

Demonstration artwork: Paul Cézanne, *Still Life with Apples and a Pot of Primroses*, ca. 1890. The Metropolitan Museum of Art, Bequest of Sam A. Lewisohn, 1951 (51.112.1). The Met lists the work as public domain in its Open Access API.

- Collection record: https://www.metmuseum.org/art/collection/search/435882
- Open Access record: https://collectionapi.metmuseum.org/public/collection/v1/objects/435882

The PDF's Blink page uses simple screen diagrams to explain the modes. These diagrams are not photographs of a physical projection. The comparison slider on the website compares actual original/threshold captures.

## Validation

The website was checked at desktop, tablet and mobile widths, in both languages. Screenshot switching, enlarged-image dialog, Escape to close and the comparison slider were exercised. PDF pages were rendered and visually inspected. Device compatibility and physical projection quality are not claimed by these presentation checks.
