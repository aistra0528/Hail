# Feature Branch Startup Performance Plan

## Branch

`feature/app-metadata-cache`

## Objective

Reduce the remaining 1-2 second delay before the app is fully usable while preserving the metadata and disk-icon cache changes. Measure the startup stages first; do not assume Room I/O is the bottleneck.

## Best persistence choice

Use Room for durable metadata and keep the in-memory map as the UI-facing hot cache. The branch now caches every installed application, not only home/checked apps, so transactional batch replacement and future indexes are a better fit than one large JSON snapshot. Room still does not improve `PackageManager` calls, root/Shizuku IPC, bitmap decoding, or RecyclerView work; those remain background and memory-cache concerns.

Use one batch Room read at startup and one transactional batch replace after the complete installed-app refresh. Keep the same in-memory cache and prefetch API so the UI architecture does not query the database.

## Work items

1. Instrument application startup, installed-app enumeration, metadata prefetch, list submission, first frame, and first complete icon bind.
2. Compare cold start with no cache, metadata snapshot, disk icons, and a warm in-memory process.
3. Publish the first list immediately, then prefetch metadata and aggressively warm icons for the entire installed-app inventory in the background.
4. Keep Room, disk bitmap, and icon-loader work on background dispatchers.
5. Bound icon decode/generation concurrency and deduplicate identical icon keys.
6. Recompute lists after cache revisions without showing a refresh indicator.
7. Validate package/version, icon-pack, adaptive-icon, user, and size invalidation.
8. Run `:app:assembleDebug` and record before/after timings.

Retain metadata for uninstalled packages with `installed=false`, hide them from Home and Apps, and restore them automatically when the package returns. The Settings page provides a confirmed action to clear both caches and rebuild the full installed inventory.

## Completion criteria

The feature is complete when the cache branch builds, cache warming does not block the first frame, stale icons cannot bind to recycled rows, runtime state invalidates on mode/freeze changes, and profiling identifies the remaining startup cost instead of guessing that Room I/O is responsible.
