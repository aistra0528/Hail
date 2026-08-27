# App Metadata Cache and Startup Performance Plan

## Branch

`feature/app-metadata-cache`

This branch combines the metadata/icon cache work with the latest `main` branch, including persistent root-shell lifecycle changes. Cache-related changes must remain isolated from unrelated local debug resources.

## Goal

Reduce repeated `PackageManager` work in home and all-apps filtering, sorting, and binding. Warm process-death starts from local cache data, keep cache warming silent, and prevent icon loading from blocking the first frame.

The observed 1-2 second delay should be measured rather than attributed to database loading immediately. Likely contributors include:

- initial installed-application enumeration;
- frozen-state checks and root/Shizuku IPC;
- label loading and sorting;
- RecyclerView binding and icon generation;
- disk bitmap decode and UI layout.

## Storage decision

### Use Room for the metadata snapshot in this phase

A database is not automatically faster. The hot path should be an in-memory map regardless of whether the durable source is JSON, SQLite, or Room. A database query during every row bind would usually be slower than the current memory lookup and would not remove `PackageManager` or freeze-state IPC costs.

The current metadata shape has grown into a complete installed-app inventory:

- read entries by package name;
- refresh a batch from `PackageManager`;
- replace the snapshot atomically;
- load it once after process creation.

That is now a better fit for Room. Room provides batch upserts, transactional replacement, schema validation, and future indexing while UI reads still use the in-memory map. Room is not queried during binding. The database is opened and read on `Dispatchers.IO`, so startup does not block on parsing or database I/O.

### Why Room is the better choice now

The branch now caches all installed applications, not only checked/home apps. Room is appropriate because the inventory is durable and refreshed in batches:

- complex queries across many metadata fields;
- package history or change records;
- multiple Android users/profiles;
- incremental deletion and indexing of a large package inventory;
- migrations and durable records beyond a replaceable cache;
- efficient replacement of the complete installed-app inventory.

Room still does not improve `PackageManager` latency or icon generation. Its role is durable, transactional storage. Read all rows in one batch, convert them to the in-memory map, and never query Room once per adapter row.

## Cache architecture

### Metadata

`AppMetaCache` owns:

- an in-memory package map for synchronous UI reads;
- explicit `prefetch()` and `prefetchPackages()` operations on `Dispatchers.IO`;
- per-package locking to collapse concurrent refreshes;
- runtime state invalidation when working mode or freeze operations change;
- a Room database with transactional batch replacement;
- a revision flow used by screens to recompute their lists.

Each row also has an `installed` flag. When a package is removed outside Hail, its last known metadata is retained for possible reinstall, but it is marked uninstalled and excluded from Home and Apps. Reinstalling the package marks it installed again and revalidates its source signature.

Static metadata and runtime frozen state must remain conceptually separate. Frozen state depends on the current working mode and is reconstructed in memory; it is not authoritative durable metadata.

### Icons

`AppIconCache` uses three stages:

1. memory `LruCache`;
2. disk bitmap cache under `filesDir/v1/icons`;
3. existing icon pack or `AppIconLoader` fallback.

Disk keys include package, user, size, package source signature, icon pack, and adaptive-icon mode. Disk I/O and bitmap decoding happen on the icon dispatcher. Writes use temporary files and rename. Recycled views verify their package tag before applying an asynchronous result.

Aggressively warm every installed icon after the first list is known, but keep this work on the background icon dispatcher and never block the first visible frame.

## Startup and UI flow

1. `HailApp.onCreate()` seeds the metadata map from the local snapshot and initializes the existing root-shell preference listener.
2. The home and all-apps flows request explicit metadata prefetch for their package set.
3. Filtering and sorting read one cache snapshot rather than repeatedly calling `loadLabel()`, package-info lookup, or frozen-state IPC.
4. Cache revisions trigger list recomputation on the main thread.
5. Freeze operations and working-mode changes invalidate affected runtime state.
6. Icon requests use memory first and perform disk/source work asynchronously.

The Apps screen refreshes the complete installed inventory on resume and on manual refresh. The Home screen rechecks retained packages on resume, so external install, uninstall, and update operations are detected without requiring Hail to perform them.

Cache warming must not toggle the existing refresh indicator or produce user-visible notifications.

## Explaining the 1-2 second delay

Before changing persistence, add timing around these boundaries:

- `HailApp.onCreate()` and `AppMetaCache.seedFromDisk()`;
- installed application enumeration;
- metadata prefetch, split into label/package-info/state timing;
- first `displayApps` or home list submission;
- first visible icon request and first completed icon bind;
- root-shell initialization and first root operation;
- first-frame and fully-bound RecyclerView timing.

Use Android Studio CPU profiler or temporary debug-only counters/timestamps. Compare four runs:

- cold process with no snapshot;
- cold process with metadata snapshot and disk icons;
- warm process with memory cache;
- cache disabled baseline.

This distinguishes JSON parsing from the much more expensive Android framework and IPC work.

## Priority optimizations after measurement

1. Keep all metadata and icon disk work off the main thread.
2. Avoid waiting for every installed package before showing the first home screen; prefetch checked/visible packages first and refresh the rest silently.
3. Use one batch metadata refresh and one filtering/sorting pass.
4. Limit concurrent icon decoding and generation.
5. Avoid repeated `applicationInfo` retrieval in bind paths where a prepared row model is sufficient.
6. Do not query Room from UI getters, comparators, or adapter binds; use the in-memory map.
7. If Room I/O is measured as a bottleneck, keep the database off the main thread and prioritize the first visible list before the full inventory refresh.

## Validation

- `:app:compileDebugKotlin` passes after merging `origin/main`.
- `:app:assembleDebug` must pass before release.
- Metadata cache misses must not start work from getters, comparators, `DiffUtil`, or adapter binding.
- Corrupt or incompatible snapshots must be ignored without startup failure.
- Freeze/unfreeze and working-mode changes must update both list flows.
- Disk icon keys must change when package version, icon pack, adaptive mode, user, or size changes.
- A recycled row must never receive another package's icon.
- Uninstalled packages remain in Room but never appear in visible Home or Apps lists.
- The Settings cache action requires confirmation, clears metadata and icons, and rebuilds every installed package in the background.
- Compare measured startup and first-visible-content timings before and after each optimization.

## Out of scope

- Querying Room directly from UI rendering paths.
- Replacing the existing root-shell implementation from `main`.
- Blocking first-frame rendering on installed-app icon prewarming.
- Persisting frozen state as permanent package metadata.
