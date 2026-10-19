# Open Track Editor — Import error-handling test fixtures

These intentionally small files exercise valid imports, recoverable coordinate/data errors, and unrecoverable XML structure errors. They are intended for regression testing and the maintainer's manual verification of the GPX/KML import-recovery pull request. The files contain deliberately malformed inputs; do not repair or auto-format them before testing.

## Directory layout

- `testing/gpx/` — eleven GPX fixtures (01–09 plus 08b and 09b).
- `testing/kml/` — eleven KML fixtures (01–09 plus 08b and 09b).
- `testing/geojson/` — reserved for the separate GeoJSON contribution; no GeoJSON tests are included in this package.

## Test procedure

1. Install the build under test and open Open Track Editor.
2. Import one fixture at a time. Compare the behavior against the expected outcomes below. The unmodified upstream version may behave differently.
3. For valid fixtures, inspect the map and export/reimport if useful.
4. For malformed fixtures, check whether a failure is reported and whether the app claims success incorrectly.
5. Once recovery UI exists, run each malformed case twice: **Keep Recovered Tracks** and **Discard Import**. Verify recovered waypoint counts, `[INCOMPLETE]` labels, and that Discard removes only tracks from that import.
6. Verify **Copy All Errors** includes file name, track, reason, and line/column when available. Verify the error list scrolls and action buttons remain accessible.
7. Repeat with a previously imported valid track already present; discarding the new import must not delete that earlier track.

## Fixture catalog and intended outcomes

| ID | GPX | KML | Intended test / target behavior |
|---|---|---|---|
| 01 | valid single track | valid single track | Successful import, 3 GPX points / 3 KML coordinates. |
| 02 | valid three tracks | valid three tracks | All 3 tracks import; GPX has 2 points per track; KML has 2 per track. |
| 03 | invalid latitude | invalid longitude text | Report bad numeric coordinate; recover other points if parser can safely continue. |
| 04 | missing longitude | missing latitude | Report missing coordinate; recover valid points when possible. |
| 05 | invalid elevation | invalid elevation | Report invalid optional altitude; preserve valid lat/lon if implementation permits. |
| 06 | multiple data errors | multiple data errors | Collect more than one diagnostic; do not claim unqualified success. |
| 07 | bad data in second of three tracks | bad data in second of three tracks | Track 1 complete, Track 2 incomplete, Track 3 complete if boundaries remain trustworthy. |
| 08 | malformed XML in second track | malformed XML in second placemark | Report structural failure; do **not** assume third track can be recovered. |
| 08b | malformed XML after visible geometry | malformed XML after visible geometry | Expanded visual recovery test. GPX: 5 complete points in first track, 3 valid points before a malformed point in second track, third track must not be read. KML: 5 valid coordinates in first placemark, 3 coordinates in malformed second placemark; recovery of second placemark depends on parser buffering. |
| 09 | truncated XML inside active track | truncated XML inside active placemark | Report document truncation, recover two valid points, and mark the affected track `[INCOMPLETE]` if kept. |
| 09b | missing root closing tag only | missing root closing tag only | Report document-level truncation; recover one complete three-point track without marking it `[INCOMPLETE]`. |

**Important:** These are regression expectations for the GPX/KML recovery branch, not claims about the unmodified upstream application. For structural errors, parsing must stop when track ownership/boundaries can no longer be trusted.

## Expected diagnostics and counts

- For 01 and 02, counts are exact expected valid input counts.
- For 03–07, inspect the reported error location and the number of **successfully persisted** points, not simply parsed points. This README does not assert exact per-fixture counts for those cases.
- For 08 and 08b, do not assume points after the structural fault are recoverable. For 08b and 09/09b, see the verified results below.
- A missing name is not included here because it is a metadata fallback test, not a parser failure; it can be added separately.

## Scope and safety

All coordinates are synthetic examples. No private trail geometry is included. The files are intentionally tiny so errors are easy to inspect in a text editor. Large-track streaming/stress fixtures should be generated separately rather than committed as enormous test assets.

This package contains **manual test fixtures only**; it does not yet contain executable automated tests or implementation changes.

## Expanded 08b visual recovery fixtures

The original 08 files are retained unchanged as minimal regression tests. The new 08b files use spatially separated tracks to make recovered lines easier to distinguish.

- **GPX-08b:** Expected on current recovery implementation: two imported tracks, eight recovered waypoints, one affected track (`GPX-08B.2 [INCOMPLETE]`); `GPX-08B.3` must not import. Confirm with SQLite, not just the map.
- **KML-08b:** Verified: two tracks, eight recovered waypoints, one affected track. The first five-point placemark survives; three coordinates from the malformed second placemark are recovered. The third placemark must not import.
- These 08b files are deliberately **not well-formed XML**. Do not auto-format or repair them before import.

## 09b — Root-only truncation (new regression fixtures)

- `gpx/09b_missing_root_close.gpx`: `trkpt`, `trkseg`, and `trk` are all properly closed; only `</gpx>` is missing. One complete track `GPX-09B` with three valid waypoints.
- `kml/09b_missing_root_close.kml`: `coordinates`, `LineString`, `Placemark`, and `Document` are properly closed; only `</kml>` is missing. One complete track `KML-09B` with three valid coordinates.
- **Expected detection:** Both imports must report malformed/truncated XML, not unconditional success.
- **Verified on the recovery branch:** Each 09b fixture reports a document-level error and offers recovery with one complete track and three points; affected-track count is zero, so the retained track does **not** receive `[INCOMPLETE]`. The diagnostic is attributed to the GPX/KML document, not the completed track.
- Retain 09 and 08b unchanged as independent regression baselines.

## Verified regression results (Android emulator)

The following cases were manually verified against `dev/import-error-handling` during development. Counts are **per imported fixture**, not totals across an app session.

| Fixture | Recovered tracks | Recovered points | Affected tracks | Diagnostic scope |
|---|---:|---:|---:|---|
| GPX-08b | 2 | 8 | 1 | Structural error in track |
| KML-08b | 2 | 8 | 1 | Structural error in track |
| GPX-09 | 1 | 2 | 1 | Active track truncated |
| KML-09 | 1 | 2 | 1 | Active track truncated |
| GPX-09b | 1 | 3 | 0 | Document-level truncation |
| KML-09b | 1 | 3 | 0 | Document-level truncation |

Keep Recovered Tracks, Discard Import, Copy All Errors, and SQLite integrity were also manually tested. A final GPX-09 Keep check showed one retained `[INCOMPLETE]` track with two points, zero orphaned waypoints, and SQLite integrity `ok`. The fixtures are **not** automated test code; the remaining cases should be run and their actual outcomes recorded by the maintainer.
