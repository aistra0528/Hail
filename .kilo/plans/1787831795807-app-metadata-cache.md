# App Metadata Cache Plan

## Goal

Remove repeated `PackageManager` work from home and all-apps filtering, sorting, and binding. Reads on the UI path should use an immutable metadata snapshot or an in-memory cache. Icons should have a disk-backed tier in addition to the existing memory cache so a cold process can display previously rendered icons without immediately asking `PackageManager` and decoding every icon again. Missing or stale metadata or icons may be loaded silently in the background; that work must never show a spinner, toast, snackbar, notification, progress indicator, foreground-service signal, or change `isRefreshing`.

The cache must not hide work inside ordinary model property getters. Loading is an explicit batch operation owned by the ViewModel or screen lifecycle. This makes the behavior deterministic, prevents duplicate requests from comparators and `DiffUtil`, and keeps synchronous getters cheap.

## Recommended storage decision

### Recommendation for this feature

Use a two-level cache:

1. A bounded in-memory `ConcurrentHashMap` or immutable map for near-zero reads.
2. A small versioned snapshot file under `filesDir/v1/` for process-death warm starts.

Keep the snapshot file as the first implementation. This feature needs point reads by package name and a full refresh, not relational queries, joins, migrations, or user-authored records. Adding a database would increase dependency size, schema/migration work, startup initialization, and test surface without improving the critical lookup operation.

The file writer must be debounced and atomic: serialize an immutable snapshot on `Dispatchers.IO`, write to `app_meta.json.tmp`, then replace `app_meta.json`. Never write directly to the live file. Bound the file size and ignore corrupt or incompatible snapshots.

### Disk icon cache

Extend `AppIconCache` with a bounded disk cache, but keep the current memory `LruCache` as the first lookup. Store encoded PNG or WebP files under a private directory such as `filesDir/v1/icons/`; never store icons in the metadata JSON. Disk reads, bitmap decoding, and icon generation must be off the main thread. The UI should use this order:

1. Memory cache: display immediately.
2. Disk cache: decode asynchronously and display as soon as decoded.
3. Package/icon loader: generate asynchronously, display, and enqueue a disk write.

There can still be a placeholder for a genuinely new icon. “No perceptible cold-start lag” means startup must not block on icon I/O or decode; it cannot guarantee that an icon never needs a first-time load. Prewarm only the home/visible all-apps packages, not every installed icon, and limit concurrent decodes so startup remains responsive.

The disk key must include package name, user ID, icon pixel size, package version/update signature, icon-pack identity/version, and rendering options such as adaptive-icon synthesis. Do not persist grayscale output because grayscale is a display state and can be applied at bind time. Remove stale files during refresh or with a bounded LRU/size-and-age cleanup. Persist failures are non-fatal and silent.

### If a database is preferred

Use **Room** rather than an embedded key-value database. Room is the best fit for this Android-only repository because it is first-party AndroidX, has compile-time SQL validation, explicit migrations, coroutine support, and a testable DAO boundary. It is more appropriate than:

- **DataStore:** intended for preferences or a single structured settings document, not a package-index table.
- **SQLiteOpenHelper:** lower-level and more error-prone than Room for no meaningful benefit here.
- **SQLDelight:** strong, but its multiplatform/code-generation advantages are unnecessary for this Android-only app.
- **ObjectBox/Realm:** viable technically, but add a third-party persistence model and dependency; no need is demonstrated by this cache.
- **MMKV or similar key-value stores:** fast for scalar keys, but provide less useful schema validation and querying than Room.

Room should be chosen now only if the project expects future metadata queries, history, package inventory, multi-user records, or additional durable app data. Room will not make `loadLabel()` or state IPC cheaper by itself; it only makes the persisted snapshot durable and queryable. The hot path still requires the in-memory map.

### Room implementation option

If Room is selected, add the Room runtime, KTX, compiler, and KSP plugin using versions compatible with the repository's Android Gradle Plugin and Kotlin versions. Add:

```text
AppMetadataEntity
- packageName: primary key
- label
- isSystemApp
- firstInstallTime
- lastUpdateTime
- flags
- enabled
- sourceVersionCode or updateTime
- localeTag
- userId
- updatedAt

AppMetadataDao
- observe/get entries by package names
- upsert a batch
- delete missing packages
- clear metadata
```

Use a singleton `RoomDatabase` created from `HailApp`, but do not query Room once per row during binding. Read the required rows as one batch, convert them to an immutable map, and let the UI use that map. Do not persist `state` in this table: frozen state depends on `HailData.workingMode` and can change outside this process. If Room is used, add a schema export and a migration test from the initial schema.

## Codebase findings

### Hot paths

1. `AppInfo.applicationInfo` calls `HPackages.getApplicationInfoOrNull(packageName)` on every access, and `AppInfo.name` then calls `loadLabel()` on every access in `app/AppInfo.kt`.
2. `AppInfo.state` calls `AppManager.isAppFrozen()`. That method can call `getApplicationInfoOrNull()` multiple times in `app/AppManager.kt` and depends on the current working mode.
3. The home flow calls `name`, `state`, and `applicationInfo` during `PagerFragment.updateCurrentList`, `NameComparator`, and `PagerAdapter.onBindViewHolder`. State also participates in `PagerAdapter`'s `DiffUtil` content calculation.
4. `AppsViewModel.filterList()` works on raw `ApplicationInfo` and calls `loadLabel()` three times per app, checks frozen state more than once, and fetches package info again for install/update sorting.
5. `AppsAdapter.bindInfo()` calls `AppManager.isAppFrozen()` and `loadLabel()` for every bind.
6. Direct label reads remain in context-menu/API/settings/shortcut/export paths. Either route those through the shared metadata reader or explicitly limit the performance goal to the two list screens.

### Existing patterns to reuse

- `AppIconCache` already provides the memory icon cache and asynchronous loader. Add disk lookup/write around that existing pipeline; do not perform disk work from `onBindViewHolder` itself.
- `HailData` and `HFiles` establish the `filesDir/v1/` convention and JSON helpers.
- `AppsViewModel` already owns list refresh and uses `viewModelScope`.
- There are currently no unit or instrumentation test directories and no Room dependency.

## Data and ownership model

Define separate types rather than one mutable entry:

```text
AppMetadata
- packageName
- label
- isSystemApp
- firstInstallTime
- lastUpdateTime
- flags
- enabled
- package/version signature
- locale signature
- userId

AppState
- NOT_FOUND, UNFROZEN, or FROZEN
- working-mode signature
- memory-only

Icon cache key
- package name
- user ID
- icon pixel size
- package version/update signature
- icon-pack identity/version
- adaptive-icon rendering signature
```

`AppMetadata` is static-ish package data and can be persisted. `AppState` is runtime data and must be invalidated when the working mode changes, when a freeze operation succeeds, when the app resumes after external operations, and when a package disappears. `NOT_FOUND` should not be persisted as a permanent fact unless it has an expiry or is removed during package refresh.

Use package name plus user/profile ID as the in-memory key. Validate persisted entries against package/version/update metadata and the current locale before using them. If the app only supports the current user, keep the user ID in the schema anyway so the contract does not silently become wrong later.

## API shape

Implement a cache/repository boundary, for example:

```text
get(packageName): AppMetadata?
getState(packageName): AppState?
prefetch(applicationInfoList): Deferred<Set<String>>
invalidatePackages(packageNames)
invalidateState(packageNames = emptySet())
invalidateAll()
currentRevision(): Long
```

`prefetch()` must:

- deduplicate concurrent requests using an in-flight map keyed by package/user;
- perform `PackageManager`, label, and freeze-state calls only on `Dispatchers.IO` or another background dispatcher;
- load a batch from disk/database before asking `PackageManager` for missing or stale entries;
- publish a new immutable map and increment a revision after successful entries are written;
- emit only package IDs or a revision, on the main thread, to a lifecycle-aware observer;
- catch per-package failures so one inaccessible package does not cancel the whole batch;
- coalesce persistence writes.

Do not launch work from `AppInfo.name`, `AppInfo.state`, a comparator, `DiffUtil`, or an adapter bind method. A cache miss in a getter should return a cheap fallback only; the screen must have already requested prefetch for its visible/list packages.

## Implementation steps

### 1. Add the storage and cache layer

Create `app/src/main/kotlin/com/aistra/hail/utils/AppMetaCache.kt` or, preferably, split persistence from memory into `AppMetadataRepository` and `AppMetaCache`.

- Keep the memory map bounded or prune entries not present in the latest installed/checked package set.
- Use a `Mutex` or single-writer coroutine for snapshot publication and persistence.
- Use atomic file replacement for the file implementation.
- For Room, use batch DAO operations and map results once per refresh; never use a DAO call per row.
- Expose a `StateFlow<Long>` revision or a lifecycle-aware callback. Never retain a Fragment or View binding.
- Deliver UI-facing emissions on `Dispatchers.Main`.

### 2. Add a disk-backed icon tier

Refactor `AppIconCache` into explicit memory, disk, and source stages without changing its public caller contract unless needed:

- Check the memory cache first.
- On memory miss, check the disk entry on the icon dispatcher, validate its key and dimensions, decode it, place it in memory, and update the `ImageView` only if the holder still represents the same package/request.
- On disk miss, use the existing `IconPack.loadIcon()` or `AppIconLoader`, place the result in memory, and encode it to a temporary file followed by atomic rename.
- Deduplicate concurrent icon loads by the full icon key so several visible rows do not generate or decode the same icon.
- Keep icon disk writes debounced or bounded by a small executor; icon generation must never delay metadata or first-frame work.
- Delete or invalidate entries when the icon pack, adaptive-icon setting, package version, or user changes.
- Preserve cancellation and recycled-view checks so a late result cannot appear in the wrong row.
- Clear both memory and disk icon caches when the icon-pack setting changes; use targeted invalidation for package updates.

Do not preload every installed icon synchronously at startup. After the first screen list is known, prewarm the checked/visible package set in the background. This gives warm cold starts without turning startup into a large disk decode job.

### 3. Initialize storage at startup

In `HailApp.onCreate()`, initialize the repository after `app = this`. Load and validate the persisted snapshot. Avoid unbounded synchronous JSON parsing or a large synchronous database query in `Application.onCreate`; if the snapshot can grow, load only the currently needed package set before the first screen and let the rest warm in the background.

The startup seed is local persistence I/O and produces no user-visible signal. It must not invoke `PackageManager` or frozen-state IPC.

### 4. Make `AppInfo` cheap and explicit

Keep `applicationInfo` as a package-manager lookup for operations that genuinely need the Android object, such as icon and launch intent handling. Add explicit metadata access for display/filtering rather than making `name` or `state` perform asynchronous work.

Possible transitional behavior:

- `name` reads the current metadata snapshot and falls back to package name;
- `state` reads the state snapshot and falls back to `NOT_FOUND` when the application is absent, otherwise a conservative state that is immediately corrected after prefetch;
- callers request `prefetch()` before rendering.

The stronger design is for list rows to bind a `DisplayApp`/snapshot object containing label, state, and package info, so filtering, sorting, and binding use the same values from one computation.

### 5. Refactor the all-apps flow

In `AppsViewModel.updateAppList()`:

1. Fetch the installed `ApplicationInfo` list.
2. Request metadata prefetch for that list without toggling the existing refresh indicator for cache warming.
3. Build a metadata map and run `filterList()` against that map.
4. On metadata revision updates, recompute `displayApps` only if the ViewModel is active and the relevant package set/query is still current.

In `filterList()`:

- evaluate `isSystemApp`, label, frozen state, install time, and update time from the snapshot;
- compute the label once per item;
- use a stable tie-breaker such as package name for equal labels/timestamps;
- avoid direct `loadLabel`, `AppManager.isAppFrozen`, and `getUnhiddenPackageInfoOrNull` calls in the filtering/sorting loop.

In `AppsAdapter`, bind the prepared row snapshot or read the immutable cache only. Do not let binding start work. Update `AppsFragment` through the ViewModel's display list; do not register a global cache listener directly on the Fragment unless it is removed with `viewLifecycleOwner`.

### 6. Refactor the home flow

Before `PagerFragment.updateCurrentList()`, prefetch the packages in `HailData.checkedList`. Filter and sort using one metadata snapshot per `AppInfo`. On a metadata revision, call `updateCurrentList()` on the main thread because metadata can change list membership and order; `notifyDataSetChanged()` alone is insufficient.

Update `PagerAdapter` so its row model contains the metadata revision or explicit state/display values used by `DiffUtil`. Otherwise the same `AppInfo` objects can produce changed state while `areContentsTheSame()` still returns true.

Remove lifecycle observers when the view is destroyed. Do not use a listener that can retain a destroyed view or call `binding` after `onDestroyView`.

### 7. Update state at the owner boundary

After `AppManager.setAppFrozen()` or a successful batch operation returns, update or invalidate state for the affected packages. Do this in `AppManager` or one central operation wrapper so home, all-apps, API actions, workers, and services cannot diverge.

Invalidate state when `HailData.WORKING_MODE` changes. Recheck state on screen resume if another process or system UI may have changed it. Package add/remove in `HailData` is not metadata invalidation; checked-list membership and metadata lifetime are separate concerns.

### 8. Handle package changes and static metadata invalidation

On explicit all-app refresh, compare the new installed list with the cached package set, prefetch missing/stale entries, and remove entries for packages no longer present unless they are intentionally retained as home-history records. Consider a package broadcast receiver only if the app needs updates while not on screen; otherwise refresh on resume and user refresh is simpler and less error-prone.

Invalidate or re-fetch labels on locale/configuration changes, package replacement, install, uninstall, and version/update timestamp changes. Include the locale and package signature in both file and Room records.

## Failure modes and protections

- Missing or corrupt snapshot: ignore it and start with an empty memory map; never crash startup.
- Partial write or process death during persistence: atomic temporary-file replacement leaves the previous valid snapshot intact.
- Duplicate misses: the in-flight map ensures one background load per key.
- Package removed during load: publish `NOT_FOUND` only for the current revision and do not permanently seed it as installed metadata.
- One package failure: record/log the failure and continue the batch; retry later with backoff or on the next refresh.
- Stale state: state cache carries the working-mode signature and is invalidated on mode changes and successful mutations.
- Stale UI event: include a revision/request ID and discard results for an obsolete query or installed-app list.
- Memory pressure: prune entries using the latest installed and checked package sets; do not rely on an unbounded map.
- Disk icon corruption or incompatible encoding: delete the individual entry and fall back to the source loader.
- Icon cache growth: enforce a byte/file-count budget and remove least-recently-used or oldest entries; cache cleanup must be background work.
- Recycled view race: associate each icon request with its package and key, and do not apply a late result to a different bound row.
- Silent-operation regression: keep cache callbacks free of UI side effects and do not call the existing refresh-state methods from cache warming.

## Validation plan

### Automated tests to add

There are currently no test source sets, so add focused unit tests for the repository/cache before wiring every screen:

1. Concurrent requests for one package perform one load.
2. Batch prefetch returns stable metadata and handles one package failure without losing other entries.
3. Corrupt, truncated, old-version, wrong-locale, and wrong-user snapshots are ignored.
4. Atomic persistence preserves the previous valid snapshot after a simulated write failure.
5. Working-mode changes invalidate state but retain valid static labels.
6. Package version changes invalidate only the affected static entry.
7. Revision events are delivered on the main dispatcher and obsolete requests cannot overwrite newer results.
8. Filtering and sorting use one metadata snapshot and make zero package-manager calls after prefetch.
9. A disk-cached icon is shown after process recreation without invoking the source icon loader for that package.
10. A package update, icon-pack change, adaptive-icon setting change, or size change does not reuse an incompatible icon.
11. Concurrent requests for the same icon key generate/decode it once, and recycled rows never receive another package's icon.

### Manual and build checks

- Cold start with a valid snapshot shows labels without per-row `loadLabel()` calls.
- Cold start with no snapshot shows a cheap fallback and then silently updates labels.
- Freeze/unfreeze updates both lists without a manual refresh and without an indicator from cache warming.
- Switching working mode refreshes frozen state.
- Install, uninstall, and package update do not leave stale labels or timestamps.
- Home filtering/sorting and all-apps filtering/sorting recompute after metadata completion.
- Launch, multiselect, import/export, and freeze-all behavior remains unchanged.
- `./gradlew assembleDebug` passes.
- Use a profiler or temporary test counter to verify that list filter/sort/bind paths no longer issue repeated package-manager calls.

## Affected files

- New: `app/src/main/kotlin/com/aistra/hail/utils/AppMetaCache.kt` and possibly `AppMetadataRepository.kt`.
- `app/src/main/kotlin/com/aistra/hail/utils/AppIconCache.kt` for disk reads/writes, keying, deduplication, cleanup, and recycled-view protection.
- `app/src/main/kotlin/com/aistra/hail/app/AppInfo.kt`.
- `app/src/main/kotlin/com/aistra/hail/app/AppManager.kt`.
- `app/src/main/kotlin/com/aistra/hail/HailApp.kt`.
- `app/src/main/kotlin/com/aistra/hail/ui/home/PagerFragment.kt`.
- `app/src/main/kotlin/com/aistra/hail/ui/home/PagerAdapter.kt`.
- `app/src/main/kotlin/com/aistra/hail/ui/apps/AppsViewModel.kt`.
- `app/src/main/kotlin/com/aistra/hail/ui/apps/AppsFragment.kt`.
- `app/src/main/kotlin/com/aistra/hail/ui/apps/AppsAdapter.kt`.
- `app/src/main/kotlin/com/aistra/hail/utils/NameComparator.kt`.
- `app/src/main/kotlin/com/aistra/hail/utils/HFiles.kt` only if atomic file replacement is added.
- `gradle/libs.versions.toml`, `app/build.gradle.kts`, and `settings.gradle.kts` only if Room is selected.

## Out of scope

- Replacing the icon source loader or changing icon-pack behavior; the change adds persistence around the existing icon pipeline.
- Changing WorkManager deferred-freeze behavior.
- Network or remote metadata; `HRepository` is unrelated.
- Persisting frozen state as authoritative durable metadata.

## Decision checkpoint

Start with the file-backed metadata repository and file-backed icon cache unless there is a confirmed requirement for relational queries or durable package history. If the team chooses a database, choose Room for metadata only, keep the same in-memory snapshot and explicit prefetch API, and keep icons as bounded files rather than database BLOBs. Do not store bitmap BLOBs in Room: that increases database size, transaction cost, and migration/backup complexity without helping bitmap decode or rendering.
