# Orientation, Aspect, and Workspace Domain Contract

Status: DECIDED

Contract version: 1

Prepared: 2026-08-28

Accepted: 2026-08-28

This is the accepted Phase 1 domain contract for the refactor described in
[PROPOSAL.md](PROPOSAL.md). It converts the broad proposal into precise domain
choices and is authoritative for subsequent refactor phases. It does not by
itself authorize UI changes.

The machine-readable companion is
[domain-contract-v1.json](domain-contract-v1.json). Current implementation
evidence remains in [BASELINE.md](BASELINE.md).

## Semantic editor source actions, HOST verified 2026-10-19

Current relation-entry update: SEMANTIC_EDITOR_SHARED_RELATIONS_HOST_20261024.
The shared Add semantic entity picker admits Orientation/Aspect for an
Orientation editor, and Orientation for an Aspect editor. Explicit confirmation
selects existing OrientationGraph types/direction or Orientation -> Aspect
BELONGS_TO/RELEVANT_TO; PART_OF and primary-Aspect constraints remain unchanged.
Aspect's inverse membership rows use the existing scoped canonical read and
same link/unlink authority. Rows open exact semantic subject IDs. Multiple
relation kinds may be authored for the same existing endpoint; no identity is
created. No generic Aspect-to-Aspect relationship is introduced. Workspace
bindings remain a separate section/authority. Successful relation refresh
preserves any unsaved title draft. Room 203 unchanged; HOST XML 42/42 in four
focused classes, production/test Kotlin compile and diff check PASS; physical
smoke pending, no deployment.

SEMANTIC_EDITOR_WORKSPACE_DELETE_HOST_20261019

The Universal Semantic Editor is a consumer, not a new lifecycle authority.
Its Workspace binding rows open exact canonical `NavTarget.Workspace(workspaceId)`
for EMBODIES/REALIZES/SUPPORTS/MONITORS; no title lookup or Context fallback.
The existing row remove icon still unbinds only the explicit relation.

The entity menu exposes source deletion through the existing
`CanonicalSemanticEntityLifecycleCoordinator.tombstone`, using a captured
canonical subject ID, ManagedSubjectType and expected version. The coordinator's
read-only `deletionTarget` capability admits Aspect and unmapped Orientation
INTENT/MILESTONE/ONGOING_STANDARD/OPPORTUNITY only; specialized/unknown kinds,
missing/deleted subjects and legacy-mapped identities fail closed. Other mandatory
Planning/PART_OF/H1/version constraints are still revalidated by the existing
atomic lifecycle command, not duplicated or bypassed by the editor.

Confirmation names the saved canonical entity and states that all of its Big
Picture appearances disappear, bound Workspaces survive and unsaved drafts are
discarded. Cancel creates nothing. Busy state prevents duplicate confirmation
and competing editor actions. Only committed success emits a single consumable
close result, bypassing the unsaved-draft prompt for the now-deleted source.
Failure retains the editor/confirmation with its error. No destructive intent or
success event is persisted in SavedState; recreation cannot replay the command.

There is no new delete implementation, structural cascade, transport or schema.
Canonical lifecycle still owns identity/relation/binding tombstones and all H1
appearance retirement atomically. Workspace navigation itself writes nothing.
Room **201 -> 201**.

HOST XML: **60/60 across 8 classes**, zero failures/errors/skips:

- `SemanticEntityDeleteViewModelTest`: 6.
- `SemanticDeletionCapabilityRoomTest`: 2.
- `CanonicalSemanticEntityLifecycleRoomTest`: 13.
- `SemanticEntityEditorViewModelTest`: 18.
- `SemanticEntityEditorNavigationContractTest`: 2.
- `EmbodyingWorkspaceViewModelTest`: 6.
- `BigPictureOperationalEmbodimentsTest`: 10.
- `BigPictureOperationalEmbodimentsRoomTest`: 3.

Both separate Kotlin compile gates and `git diff --check` PASS. Physical acceptance
is pending: open a bound Workspace from the editor; on disposable ordinary
Orientation/Aspect test Cancel, then confirmed source deletion; verify editor
closes, Library/Big Picture update, bound Workspace survives and restart does not
resurrect the source. Deploy only through the user's `make exp-arm64-cycle`.

## Architecture epoch note

This file is the accepted **Canonical V1** domain contract.

Its Workspace hierarchy clauses, including `parentWorkspaceId`, remain
CURRENT authority until an explicit Canonical V2 hierarchy cutover
completes.

The accepted V2 hierarchy target is defined separately in
`HIERARCHY-PLACEMENT-CONTRACT-V2.md`.

The V2 decision does not silently supersede this contract's
`ManagedSubject`, Orientation, Aspect, Workspace identity,
WorkspaceBinding, capability, semantic-relation, sync, or lifecycle
ownership.

Cross-epoch authority and migration discipline are governed by
`../../governance/PROJECT-CONSTITUTION.md`.

## Decision summary

1. `ManagedSubject` is a constrained identity root with exactly two subtype
   families: Orientation and Aspect. Workspace is not a ManagedSubject.
2. Context evolves through compatibility into Workspace, but existing Contexts
   are classified individually rather than automatically turned into
   Orientations or Aspects.
3. Main Beacon, Main Beacon Group, Goal, Direction, reusable Day Theme
   definition, and manual Arc Quest receive canonical Orientation identities.
4. Main Beacon Group owns its own assessment independently from its members.
5. Aspect is a stable domain/lens, supports one-parent hierarchy in contract
   v1, and does not receive Orientation Impact or Importance.
6. One subject may have at most one primary `EMBODIES` Workspace and any number
   of non-primary supporting bindings. One Workspace may embody at most one
   subject.
7. Semantic ownership, placement, relation, commitment, and execution remain
   separate persistence concepts.
8. Capability instances are typed, versioned, independently owned modules.
9. Orientation assessment axes are categorical and ordered. Legacy numeric
   values remain preserved and are projected using explicit mapping rules.
10. Shared filtering uses a versioned, typed, fail-closed AST.

## 1. Identity and subtype ownership

### ManagedSubject

`ManagedSubject` owns only fields common to stable semantic subjects:

```text
id: UUID string
subjectType: ORIENTATION | ASPECT
title
description
createdAt
updatedAt
syncedAt
version
isDeleted
```

Allowed subtype rows are exactly:

```text
Orientation(subjectId, kind, lifecycle, ...)
Aspect(subjectId, parentAspectId, order, archived, ...)
```

The subtype shares the ManagedSubject primary key. A subject must have exactly
one matching subtype. Workspace, Mission, Day Task, Activity, document,
attachment, capability instance, placement, and relation cannot be inserted as
ManagedSubject subtypes.

Persisted IDs are UUID strings. New subjects use random UUIDs. Legacy source
entities receive deterministic UUIDv5 IDs from a fixed ForwardApp namespace
and the tuple `(sourceType, sourceId)`. A durable unique mapping stores source
type, source ID, subject ID, migration version, and mapping state. Source IDs
are never assumed globally unique across tables.

Unknown future subject types or Orientation kinds are retained on transport
with their raw code and quarantined from mutation. They are not coerced to a
known default.

### Initial Orientation kinds

Contract v1 recognizes:

```text
MAIN_BEACON
MAIN_BEACON_GROUP
GOAL
DIRECTION
MILESTONE
ONGOING_STANDARD
OPPORTUNITY
DAY_THEME
ARC_QUEST
```

Existing entities are never heuristically reclassified into `MILESTONE`,
`ONGOING_STANDARD`, or `OPPORTUNITY`. Those kinds are available for explicit
new creation or later user-directed conversion. Existing backlog Goal maps to
`GOAL`; a manual Arc Quest maps to `ARC_QUEST`.

Kinds express semantic role, not size, hierarchy, Workspace role, lifecycle,
or planning level.

### PROJECT canonical vocabulary

CANONICAL_PROJECT_ORIENTATION_HOST_20261025 makes the accepted architecture-vision
PROJECT kind current for explicit native authoring. PROJECT is a bounded
undertaking intended to reach a recognizable completed state. Its identity is
ManagedSubject ORIENTATION + Orientation(kind=PROJECT), not a Workspace, Planning
entity or task. Common lifecycle (including REALIZED), all current applicable
Orientation axes (including EFFORT/COST/RISK), scoring and semantic tags apply.
Aspect BELONGS_TO/RELEVANT_TO, Orientation graph, PlanningCommitment and optional
Workspace EMBODIES/REALIZES reuse existing authorities. No operational collections
belong to PROJECT. Generic canonical semantic deletion supports native PROJECT.
No title/tag/role/Context/H1 heuristic creates or reclassifies a Project; no
legacy migration/adoption or product kind-transform command is introduced here.
The enum is appended; persisted/transport kind remains a stable string, with no
schema or transport-version change. Shared unknown-kind resolution retains raw
code; current Android canonical ingress still rejects unknown kinds atomically,
never silently defaults to PROJECT. Room 203; HOST XML 98 app + 21 shared PASS,
both separate Kotlin compile gates PASS; physical smoke pending.

## Canonical native Aspect -> Orientation conversion

SEMANTIC_SUBJECT_CONVERSION_HOST_20261025

Status: IMPLEMENTED / HOST VERIFIED; physical acceptance pending. Room 203 -> 204
adds managed_subject_conversions and indexes only; existing identities receive
no fabricated conversion history. Semantic editor menu "Перетворити на орієнтир…"
opens an impact confirmation with explicit kind selection and no default.

### Identity and authority

Aspect -> Orientation changes ManagedSubject.subjectType while retaining the
same canonical subject ID, title and description. The current subtype changes
from Aspect to Orientation; an obsolete Aspect row must not remain as a second
active subtype. An immutable, versioned conversion fact preserves former type
and taxonomy evidence separately from the current subtype. This fact is
semantic lifecycle/provenance state, classified SEMANTIC_NOT_HIERARCHY; it is
not H1, a legacy mapping, a new semantic identity or a universal mutation service.
Changing an OrientationKind within Orientation is a separate operation, now
implemented by the bounded command below. Reverse subject conversion remains
unsupported.

The first product slice admits only live native unarchived Aspects. Historical/specialized
subjects, legacy-mapped identities and unhandled dependent-state shapes fail
closed. The user explicitly selects an ordinary supported OrientationKind,
from PROJECT, INTENT, MILESTONE, ONGOING_STANDARD and OPPORTUNITY;
compatibility/specialized kinds must not appear merely
because they are enum members. A canonical conversion policy owns the exact
allowlist and preflight, not Compose. No title, tag, Workspace, role or placement
heuristic determines the new kind.

### Preservation and retirement

- Preserve the subject identity/title/description and visible semantic tags.
  Tag membership remains typed: retire former ASPECT tag rows and establish
  ORIENTATION memberships through their authority; never mutate a tag row's
  immutable typed tuple/identity in place. Historical memberships remain durable.
- Preserve compatible WorkspaceBindings because both endpoint classes are
  ManagedSubjects. Never delete/recreate a bound Workspace, Context or capability.
- Preserve every exact H1 placement ID, target subject ID, PRIMARY/LINK role,
  parent/container membership and order. Conversion creates no appearance and
  never substitutes a Workspace target for the semantic target.
- Retire AspectOrientationRef memberships that use this Aspect through versioned
  tombstones. Do not translate them into Orientation graph edges or new
  BELONGS_TO memberships. Preserve their history through conversion evidence.
- End the source's former Aspect-parent relationship, retaining its previous
  state as history. Promote direct child Aspects to the former parent, or root;
  children append to the destination sibling set through CanonicalAspectRepository
  move, retaining relative child order and identities; descendants survive.
  Do not derive semantic meaning from H1 while doing this.
- Retarget supported live typed PlanningReferences to the same ID as ORIENTATION,
  preserving SOURCE/RELATED intent through the canonical relation lifecycle.
  Retain retired ASPECT reference history. Do not create PlanningCommitment,
  a new Planning item or another semantic identity. Recurrence SOURCE and any
  other typed dependent formats require an explicit closure policy before
  admission; V1 rejects a nondeleted TASK recurrence template with this Aspect
  SOURCE until it is explicitly detached or changed. An unhandled dependency
  blocks conversion rather than becoming an orphan or changing future creation.
- Establish the normal target-kind initial assessment/lifecycle through the
  canonical Orientation aggregate authority. No Aspect scoring is invented and
  no Workspace scoring/tags are inherited.

### Durable fact and transaction

The minimum conversion evidence identifies the subject, former/new types,
explicit target kind, source/committed subject versions, former Aspect taxonomy
state and canonical timestamp/version/sync metadata. Event identity must be
stable for replay; facts are immutable, not user-deletable presentation records.
They survive source deletion and cannot be dropped by a stale ACK or replay.
Table: managed_subject_conversions; formatVersion=1, immutable version=1. Event
ID is `<subjectId>:ASPECT:ORIENTATION:<fromSubjectVersion>`, committed subject
version is source+1. The row records target kind, previous parent/order/archive
state and timestamps/sync metadata. There is no fact-delete lifecycle command.

One Room transaction owns subject/subtype transition, conversion fact,
assessment establishment, child promotion, relation/tag retirement and supported
typed-reference retargeting. Final validation failure rolls back all changes;
no compensation/repair pass may be required to make a committed state valid.
Preflight confirmation captures the exact source/version and impact counts;
the command revalidates dependencies. Cancellation creates nothing, duplicate
submit/replay cannot create a second conversion, and navigation restoration
must never replay the command. The exact same editor route reloads the converted
Orientation after success or restart.

### Transport and historical endpoint admission

Conversion facts are canonical durable state: include them in full snapshot,
exact Restore, peer Merge, dirty/changedSince transport and exact-version ACK.
V1 selective import rejects snapshots with conversion facts and directs the user
to full Restore/Merge; internal metadata helpers do not bypass this public gate.
History-bearing snapshots use wire snapshotVersion="semantic-subject-conversion/1"
with canonicalSnapshotBaseVersion retaining numeric 1/2. New adapters verify and
normalize this generation; old integer decoders reject before silently stripping
history. Snapshots without facts retain their existing numeric generation.
Wrapped and raw Android imports share this adapter; explicitly marked canonical
snapshots never fall through Desktop subset decoding. Unknown generations and
nonempty history under an unversioned/numeric envelope fail closed.

Only a validated conversion fact can admit tombstoned ASPECT endpoints whose
current subject is ORIENTATION. Live endpoints still require the current exact
type/subtype. Without valid conversion evidence existing strict validators remain
unchanged. Higher-version identity changes need matching conversion evidence;
stale pre-conversion payloads cannot restore an Aspect subtype, active old
memberships or obsolete typed references. Equal-version divergent conversion
facts fail closed; identical replay is inert. Legacy payload absence is not
inferred conversion history and cannot authorize a cross-type change. Format 1
admits one native conversion per subject, not a general multi-step type chain.
Deleted child Aspects may retain historical parent IDs only with verified fact;
live children must be promoted. Live former-ASPECT tags/Planning refs/memberships
or recurrence SOURCE dependencies invalidate final closure.

Migration 203 -> 204 adds only necessary conversion persistence/indexes. Existing
subjects are not converted, classified or given fabricated conversion records.
HOST XML: 83 app + 20 sync + 23 shared JVM tests PASS, including migration,
production exact Restore, peer Merge, stale/wrong-type rejection, immutable
divergence/replay, late-write rollback, promotion overflow, ACK, raw/wrapped file
import and editor recreation. Both separate app Kotlin compile gates PASS.
Selective import rejection is deliberate; no physical acceptance is claimed.

## Canonical native OrientationKind transformation

ORIENTATION_KIND_TRANSFORMATION_HOST_20261025

The editor's explicit "Змінити тип орієнтира…" delegates to
CanonicalOrientationKindCoordinator; it is not a freely editable enum field.
Source and target must differ and belong to INTENT, PROJECT, MILESTONE,
ONGOING_STANDARD or OPPORTUNITY. Only live native unmapped Orientations are
admitted, including former native Aspects with verified conversion history.
Specialized kinds/legacy mappings/unknown lifecycle codes fail closed.

One transaction revalidates the captured subject/current-assessment versions and
Planning/recurrence admission, writes a new kind/current assessment/revision and
immutable fact, then validates the complete semantic closure. Cancel and SavedState
restoration do not mutate; duplicate captured intent is idempotent, conflicting
intent/races reject; any late write/validation failure rolls back the whole change.
ManagedSubject ID/type, Orientation identity, title/description, lifecycle, tags,
Aspect memberships, graph edges, WorkspaceBindings/Workspaces, H1 PRIMARY/LINK
appearances/order/parents, Planning participation/references and recurrence SOURCE
are unchanged. No identity creation, Context adoption or Planning scheduling occurs.

Assessment axes with unchanged applicability retain values/origins/provenance;
changed applicability resets to initialOrientationAssessment(target) defaults.
For these five kinds only Expected span/Target window can change: entering
ONGOING_STANDARD means DERIVED ONGOING / NOT_APPLICABLE; leaving means UNSET.
Scoring overrides and evaluation state survive. Old immutable revisions retain
their exact historical source kind; current assessment must match current kind.

Room 204 -> 205 adds nullable fromOrientationKind and
previousAssessmentRevisionIdsJson columns to managed_subject_conversions only.
No existing rows are reclassified or rewritten. Format 1 still represents native
Aspect -> Orientation. Format 2 represents ORIENTATION -> ORIENTATION kind change:
ID `<subjectId>:ORIENTATION_KIND:<sourceVersion>`, immutable version 1,
committed subject version source+1, explicit from/target kinds and exact prior
revision IDs. Deterministic result revision is `<subjectId>:kind:<sourceVersion>:assessment`.
Facts are SEMANTIC_NOT_HIERARCHY provenance, not a new current-kind authority.
Ordered versions must form a consistent kind chain; current subtype/kind and
historical revision ownership are verified. Live/deleted source history remains
transport-visible; incompatible branches and equal-version divergent facts reject.

Full snapshot/Restore/Merge, dirty/changedSince and exact-version ACK reuse the
existing conversion stream. Generation semantic-subject-conversion/2 is required
for kind facts; new readers retain generation 1 support, generation-1 Android
readers reject generation 2 instead of stripping it. Numeric-envelope or
generation-1 downgrade of kind facts rejects. History-bearing selective import
remains blocked in V1; use full Restore/Merge. Physical acceptance is pending.

HOST evidence: 106 app / 14 classes, 22 sync / 3 classes, 34 shared JVM / 6 classes;
zero failures/errors/skips. Exact classes/counts are in STATE.md. Both separately
run app Kotlin compile gates, shared Kotlin/JS compile and git diff --check PASS; no full-suite or device
acceptance claim.

## 2. Assessment axes

Every axis is nullable. Null plus a value origin is meaningful; there is no
fake numeric zero and no catch-all `NONE` enum value.

### Value origin

```text
EXPLICIT        user or authoritative import set the value
INHERITED       resolved through a declared inheritance rule
DERIVED         deterministically mapped or calculated from another value
UNSET           applicable but no value is known
NOT_APPLICABLE  the axis does not semantically apply
```

Contract v1 defines no automatic inheritance rule. `INHERITED` is reserved
until a relation-specific rule is accepted. A stored effective value must
retain its origin and provenance reference.

### Ordered values

| Axis | Low to high / near to far values | Meaning |
| --- | --- | --- |
| Importance | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` | How unacceptable non-realization would be |
| Impact | `TINY`, `SMALL`, `MEDIUM`, `LARGE`, `TRANSFORMATIVE` | Magnitude of change caused by realization |
| Breadth | `LOCAL`, `AREA`, `SYSTEM`, `MULTI_SYSTEM`, `LIFE_WIDE` | Breadth of life/systems affected |
| Expected span | `INSTANT`, `DAYS`, `WEEKS`, `MONTHS`, `YEAR_PLUS`, `ONGOING` | Expected calendar span, not work effort |
| Target window | `NOW`, `THIS_WEEK`, `THIS_MONTH`, `THIS_QUARTER`, `THIS_YEAR`, `LATER`, `SOMEDAY` | Intended realization window |
| Attention tier | `P3_LATER`, `P2_NEXT`, `P1_ACTIVE`, `P0_NOW` | Strategic attention allocation now |
| Commitment | `IDEA`, `CANDIDATE`, `INTENDED`, `COMMITTED`, `OBLIGATION` | Strength of decision/obligation |
| Confidence | `SPECULATIVE`, `POSSIBLE`, `LIKELY`, `CONFIDENT`, `CERTAIN` | Confidence that the Orientation is right |

Ordering is used only within the same axis. No general arithmetic is implied
between categories.

### Applicability

All eight axes apply to `MAIN_BEACON`, `MAIN_BEACON_GROUP`, `GOAL`,
`DIRECTION`, `MILESTONE`, `OPPORTUNITY`, and manual `ARC_QUEST`.

For `ONGOING_STANDARD`, Expected span is deterministically `ONGOING`; Target
window is not applicable. The other axes apply.

For `DAY_THEME`, Importance, Impact, Breadth, Commitment, and Confidence apply
to the reusable definition. Expected span, Target window, and Attention tier
are not applicable to the reusable definition. Daily activation, budget,
order, and optional day-specific Attention tier belong to DayTheme assignment.

Aspect receives none of these Orientation axes. Future Aspect health, desired
attention share, review cadence, or balance are separate models.

### Lifecycle and specialized state

Common Orientation lifecycle is:

```text
EXPLORING
READY
ACTIVE
PAUSED
REALIZED
DROPPED
```

Lifecycle is nullable during compatibility projection. `BLOCKED` and `WAITING`
are not lifecycle values: they describe impediment/progress condition and may
later receive a separate common model. Until then, native blocker/readiness
fields remain authoritative.

The following never map into common lifecycle authority:

- Main Beacon readiness or level status;
- Mission operational status/priority;
- Day Task status/priority/completion;
- DayTheme assignment activation;
- recurrence-series lifecycle;
- Aspect health or attention balance.

## 3. Aspect contract

An Aspect answers which stable domain or lens an item concerns. It has one
optional parent Aspect in contract v1, sibling order, archived state, standard
sync metadata, and tombstone. The hierarchy is acyclic and deleting a parent
does not delete descendants; descendants become root candidates until moved or
the deletion is reversed.

Aspect taxonomy UI consumer (SEMANTIC_ASPECT_PARENT_UI_HOST_20261024): the
Semantic Editor exposes current parent/exact editor navigation, an existing
canonical Aspect picker and Make root, with explicit confirmation. Self, current
parent and transitive descendants are excluded; CanonicalAspectRepository.move
still owns final transactional admission/version/dirty updates. Children and H1
placements remain unchanged. Aspect.parentAspectId is SEMANTIC_NOT_HIERARCHY and
is not replaced or duplicated by BELONGS_TO in this slice. Room 203 unchanged;
HOST XML 39/39 in five focused classes; production/test Kotlin compile and diff
check PASS; physical smoke pending. This UI does not decide a future authority
consolidation.

Direct-child read/navigation update (SEMANTIC_ASPECT_CHILDREN_HOST_20261024):
the Aspect editor also lists direct live taxonomy children, ordered by
aspectOrder then exact ID, each opening its exact SemanticEntityEditor. The
existing taxonomy snapshot is reused with no per-row reads or new persisted
list. Resume refresh reflects child rename/reparent/tombstone while preserving
the current title draft and pending confirmation. H1 nesting is not membership.
Room 203 unchanged; HOST XML 39/39 in four focused classes; production/test Kotlin
compile and diff check PASS; physical smoke pending, no deployment.

Orientation-to-Aspect relations are:

```text
BELONGS_TO   taxonomic membership
RELEVANT_TO  useful cross-domain relevance without classification
```

An Orientation can belong to multiple Aspects but has at most one primary
`BELONGS_TO` relation. `RELEVANT_TO` cannot be primary. Relation rows are
ordered within an Aspect and are versioned/tombstoned.

Tags remain lightweight strings. There is no automatic tag-to-Aspect promotion
or synchronization. An explicit conversion command may later create an Aspect
and relations while preserving the original tag until separately removed.

### Canonical semantic tag membership (Room 202)

CANONICAL_SEMANTIC_TAGS_HOST_20261019

ManagedSubjectTagRef owns Orientation/Aspect membership: explicit subjectType,
subjectId, normalizedTag, deterministic ID, timestamps, version, syncedAt and
tombstone. ManagedSubject IDs are globally unique; wrong typed references fail
closed. Tags are lightweight labels, not Aspect taxonomy or semantic edges.
CanonicalSemanticTagRepository owns authoring/bulk observation; live reads join
exact owner type, subtype existence and lifecycle. Generic source deletion also
retires memberships atomically. Workspace tags remain a separate authority;
no copy/inheritance via bindings, Context, H1 or Planning.

Normalization trims whitespace/leading hashtag markers and lowercases invariantly;
it is idempotent. Membership ID is `subject-tag:` plus UUID.nameUUIDFromBytes of
UTF-8 `v1\0type\0subjectId.length:subjectId\0normalizedTag`. Re-adding revives the
same row with a new version; duplicate live Add is inert. Removal is a tombstone.
Native migration 201->202 creates no memberships from existing data.

SnapshotBundle.managedSubjectTagRefs is additive/nullable: absent peer stream
does nothing; exact full Restore of an older backup has no semantic tags.
Exact Restore retains versions/syncedAt; Merge dirties incoming winners, rejects
equal-version divergence and ignores lower versions. Exact-version ACK cannot
clear newer changes. Live tags never surface a tombstoned owner, even when an
older specialized lifecycle retained an unretired historical row.

## 4. Workspace and binding contract

Workspace owns operational identity and organization:

```text
id
nameOverride?
descriptionOverride?
parentWorkspaceId?
roleCode?
order
lifecycle/configuration metadata
```

Workspace hierarchy is one-parent and acyclic in v1. It does not mirror Aspect
or Orientation hierarchy.

Legacy Context semantic retirement preserves the existing Workspace hierarchy
rather than translating `Context.parentId` into Aspect hierarchy. Context-tree
cutover is bottom-up: active descendants retire before their parent Context.
A canonical child Workspace may therefore remain under a still
`CONTEXT_BACKED` parent Workspace; later parent cutover preserves that same
Workspace id and hierarchy edge. Parent-first cutover is not supported.

Bindings are:

```text
EMBODIES  primary cohesive working surface for one subject
REALIZES  work directly contributes to realization
SUPPORTS  work contributes indirectly or operationally
MONITORS  workspace observes/reviews the subject
```

Only `EMBODIES` may be primary. A subject has zero or one primary embodied
Workspace; a Workspace embodies zero or one subject. Other binding types are
many-to-many, ordered, and non-primary.

The ManagedSubject owns semantic title and description. An embodied Workspace
defaults to displaying that title and description without copying them.
Workspace can store an explicit operational name/description override, but the
override does not rename the subject.

In a combined presentation:

- page title and semantic summary come from ManagedSubject;
- breadcrumb and navigation placement come from Workspace hierarchy;
- semantic edit actions target the subject;
- backlog/inbox/problem/document actions target the active Workspace
  capability;
- specialized actions remain owned by the specialized entity;
- existing Context routes remain compatibility entry points until an explicit
  navigation migration.

### Current Workspace operational destination (R2D.4)

The current product entry is `Workspace(workspaceId)`, including from Library.
It reuses the Context-era operational UI behind canonical Workspace admission.
Workspace existence, visible operational name/description and Properties route
come from Workspace, independently of an optional live compatibility Context.
`sourceContextId` never substitutes for route identity, and standalone/System
Workspaces must not manufacture Context rows to render. The same-key live-shell
adapter remains a deliberately limited compatibility operation boundary.

Canonical title/description and tombstone changes are observed from the exact
Workspace row. The semantic combined-presentation rules above remain separate:
this operational destination does not rename the embodied subject or own its
lifecycle. H1, after its explicit cutover, owns structural arrangement; old
Context hierarchy and GroupScope do not acquire authority through this screen.
Opening the Workspace route does not create Direction links from H1 children.
Explicit legacy routes retain their compatibility policy until separately retired.

Workspace Properties is one existing tabbed properties surface, keyed by
workspaceId. Legacy edit-route aliases admit the canonical Workspace directly;
there is no second Context-settings product screen or navigation command.
General/Markdown/tags, semantic bindings, display, capabilities, reminders and
registered capability settings share UI, not persistence ownership. Each family
keeps independent commands; header Save owns Workspace name/description only.
Structural preset and scoring/Evaluation controls are excluded. Existing presets
and semantic scores are neither deleted nor copied onto Workspace.
Context.relatedLinks are not promoted to Workspace scalars or semantic bindings.
Workspace reminder identity uses the existing PROJECT transport type; type-scoped
reading/clearing must preserve other entities sharing the same raw ID.
No persistence/schema or semantic/Planning/H1 mutation authority changes here.

### Workspace dashboard semantic references (R2D.6A)

Presentation update: WORKSPACE_ENTITY_HUB_HOST_20261025 supersedes the header
pill from WORKSPACE_DASHBOARD_REFERENCE_LAYOUT_HOST_20261024. Dashboard now puts
the view strip immediately below the Workspace title, then a compact Entity hub,
then Connections. Hub previews at most six canonical rows (two per group); full
Related entities shares the same grouping and exact typed navigation. EMBODIES
is labelled semantic source; source Aspect relations/taxonomy, other Workspaces
bound to that Aspect, Planning and direct Workspace bindings stay distinct.
Since WORKSPACE_SEMANTIC_CONTEXT_HOST_20261025 source expansion admits a unique
live typed EMBODIES Aspect or Orientation. Aspect shows its immediate taxonomy
and direct relations; Orientation shows canonical Aspect memberships and incoming,
outgoing/mutual semantic graph relations. No second relation hop, Workspace
taxonomy, H1/Context/title inference or copied relationship is introduced.
Source-labelled taxonomy/relations edit actions delegate to the exact existing
Universal Semantic Editor. No command executes merely on navigation; actual
confirmation/mutation remains in its canonical Aspect/links/graph repositories.
No source or ambiguous/deleted source means no semantic edit action or fabricated
hierarchy. The preview and full list react to canonical changes without refresh.
Move in Navigation != Change Aspect parent != Change Orientation BELONGS_TO.
HOST XML 95/95 in 10 classes, production/test Kotlin compiled; Room 203 unchanged,
physical acceptance pending. All relations opens the enabled capability view, otherwise
a read-only dialog without changing capability state. Empty/disabled-capability
Workspaces still have a valid preview. No new persistence or mutation authority.
HOST XML 33/33 in six focused classes; production/test Kotlin compiled; Room 203
unchanged, physical smoke pending. The view strip retains all capability-admitted
views and counts; it does not change persisted view ordering/preferences.
Connections has a compact preview and plus delegated to the existing picker
and canonical connection commands. The semantic projection itself remains
read-only. The operational bottom panel is unchanged. Room 203 unchanged;
HOST XML 21/21 in five focused classes, production/test Kotlin compile and
diff check PASS; visual/physical smoke pending, no deployment.

The current canonical Workspace dashboard consumes the existing WorkspaceBinding
read model and lightweight canonical entity catalog, keyed by canonical workspaceId.
It shows Orientation/Aspect canonical titles and classes; binding kinds remain
EMBODIES/REALIZES/SUPPORTS/MONITORS, never inferred from visual placement or
operational configuration. Multiple binding types share one semantic subject row.
Locale-independent title/class/id ordering and stable relation labels are used.

Row taps route the exact subjectId to the existing Universal Semantic Editor.
Properties binding changes and canonical title/lifecycle changes are observed
reactively without refresh or restart. Empty bindings are valid. Under the
existing fail-closed projection, unavailable/ambiguous targets and malformed
binding kinds do not expose navigation; there is no resurrection or new read-side
cleanup policy. Standalone and Context-backed Workspaces use the same authority.

The semantic dashboard projection is read/navigation only. It does not mutate WorkspaceBinding,
Context, H1/Big Picture, GroupScope, MainBeacon, Planning, capabilities, reminders,
Connections or lifecycle. Existing Properties and bootstrap commands retain their
separate ownership. No schema/transport change; Room 200. HOST XML 49/49 tests in
10 classes and both separate Kotlin compile gates PASS; physical acceptance pending.

### Workspace Related entities capability

WORKSPACE_DASHBOARD_MATCHED_CARDS_HOST_20261026: the Dashboard section title is
also Semantics (no conditional Semantic structure title). Semantics/Connections
share only themed card/header chrome; Connections keeps its separate operational
data, six-item preview, exact open/add actions and full-view admission. No
relationship authority is merged. Room 205; HOST XML 10/10 PASS, visual smoke pending.

SEMANTICS_LABEL_DECK_ORDER_HOST_20261026: the current user-facing capability/view
name is Semantics. The stable capability/view/route remains relations/RELATIONS;
old Related entities and Зв’язки label aliases are accepted for existing settings.
Connections remains distinct. Earlier checkpoints below use the former product
label; no relation authority, admission or persistence semantics changed.

WORKSPACE_DASHBOARD_SEMANTIC_STRUCTURE_HOST_20261026 supersedes only the old
Dashboard Entity hub row preview with a compact card. It reuses the existing
relationship-panel grouping, exact verified linked semantic source and first
three nonempty group summaries with complete counts. Source opens its editor;
group/View all discloses the full Related entities view, or existing dialog when
disabled. No default enabling, relation mutation or new read authority. The full
panel/Connections/chrome are unchanged. HOST XML 19/19 PASS; Room 205 unchanged;
visual physical acceptance pending.

WORKSPACE_RELATIONSHIP_PANEL_HOST_20261025 changes only the dedicated view's
presentation: theme-derived source tiles/EMBODIES bridge, counted semantic groups,
branch connectors and tappable typed cards. Source tap delegates the existing
semantic edit action; directions/multiple kinds/unavailable navigation remain
visible. The panel uses precisely the existing Entity hub rows and exact targets;
it does not turn semantic taxonomy into H1. Missing/ambiguous embodiment has no
fabricated source. Dashboard preview and surrounding Workspace chrome are not
restyled. HOST XML 33/33 in four presentation/ViewModel classes PASS; Room 205
unchanged, visual physical acceptance pending.

WORKSPACE_RELATIONS_CAPABILITY_HOST_20261020

RELATIONS (`Semantics`, formerly `Related entities` / `Зв’язки`) is a PRESENTATION capability: a read projection of existing
canonical relations, not a new relationship authority or universal graph.
WorkspaceBinding owns Workspace -> Orientation/Aspect EMBODIES/REALIZES/SUPPORTS/
MONITORS. PlanningReference owns Planning owner -> Workspace SOURCE/RELATED;
the Workspace-facing list is its inverse read, not PlanningCommitment participation.
Included Planning owners: SCOPE, TACTICAL_MISSION, ARC_QUEST, DAY_TASK, DAY_FOCUS.

WorkspaceRelationsRepository composes bulk binding/catalog reads and a scoped
SQL reference/owner projection with live Workspace/owner/relation admission.
Typed family + endpoint kind + ID preserves identity, deterministic ordering and
direction; multiple kinds share one endpoint row within a family. Missing/deleted
or ambiguous semantic targets are excluded, never resurrected or guessed.
No Context provenance, H1, tags, Connections, raw-ID or title inference.

Semantic rows navigate exact subject IDs to Universal Semantic Editor. DAY_TASK
uses exact EditTask(taskId). Other Planning subtypes have no exact NavTarget;
their rows remain visible with navigation disabled, no overview/title fallback.
Reactive title, binding/reference, lifecycle and Restore/Merge changes need no
manual refresh. The projection never authors or cleans up domain relations.

Capability registration/settings use the existing kernel and
workspace_capability_instances (SEMANTIC_NOT_HIERARCHY presentation metadata).
RELATIONS has maxActive=1, default instance key, strict configuration v1 `{}`,
no owned collections, dependencies, legacy migration mapping or search identity.
Enable/disable/archive/restore/delete, order/version/dirty state, exact-version
ACK and full canonical transport follow the existing kernel. Disable/archive/
delete hides only this capability; domain relations remain. No new tables,
schema migration or persisted derived list: 202 -> 202.

The original unified Dashboard card and later header/link presentation are
superseded by the compact Entity hub above; detailed relations remain in the
dedicated view. Since
WORKSPACE_RELATIONS_VIEW_HOST_20261024, RELATIONS also exposes the ordinary
Workspace `Related entities` view; quick entry selects this same view, not a dialog.
Canonical instance lifecycle controls admission directly without a legacy
capability mapping, including standalone and System Workspaces. Saved selection
uses the existing view preference; disabling resolves to an available view.
The view has no creation input and reuses the card's read/navigation projection.
Disabled/default: retain the read-only Entity hub preview/full-list dialog,
without enabling the capability; never render duplicate dashboard cards.
Editors/Properties retain relation mutation ownership.

View integration HOST XML: 55/55 in six classes, zero failures/errors/skips;
production/test Kotlin compiled in the focused task. Room 203 unchanged.
Physical view acceptance pending; no deployment.

WORKSPACE_RELATED_ENTITIES_ASPECT_HOST_20261024 extends only this read lens:
retain direct Workspace relations and show a separate block for direct relations
of its unique live EMBODIES Aspect. Admitted families: AspectOrientationRef
BELONGS_TO/RELEVANT_TO, Aspect.parentAspectId immediate parent/children, other
WorkspaceBindings to that Aspect, and inverse typed PlanningReference
SOURCE/RELATED with live owners/Aspect. The current Workspace's own binding is
already shown directly and is not duplicated in the Aspect block. Directions
and exact typed endpoint navigation are preserved; unsupported Planning routes
remain disabled. Aspect title opens its exact Universal Semantic Editor.
Only one hop to the Aspect is admitted; no expansion through Orientation
embodiment, other binding kinds, ambiguous/missing/deleted embodiment or a second
relation hop. Empty related state is valid. No H1/Context/title/raw-ID inference,
new relation authority, copying, writes, tables or transport changes. Existing
canonical repositories plus scoped inverse Planning SQL supply reactive reads;
there are no per-row reads. Stable capability/view IDs remain unchanged.
Room 203; HOST XML 50/50 in seven classes, production/test Kotlin compile and
diff check PASS. Physical smoke pending; no deployment.

HOST XML 75/75 app + 5/5 shared; both separate Kotlin compile gates PASS.
Physical acceptance pending; no deployment.

### Semantic Editor -> new embodying Workspace

`SemanticEmbodyingWorkspaceCoordinator.create` is an explicit product command,
not generic semantic creation or Workspace conversion. It accepts the exact
live Orientation/Aspect subject type/id/version and confirmed Workspace overrides.
Saved semantic title/Markdown are editable defaults only; operational overrides
do not rename the subject or create ongoing title-copy synchronization.

One Room transaction joins CanonicalWorkspaceRepository.createIdentity and
CanonicalWorkspaceBindingRepository.bind(EMBODIES). Identity creation reuses the
normal standalone Workspace writer, without the H1 occurrence authored by the
existing create/placement workflow. Source identity/data remain unchanged.
Only the Workspace and explicit primary binding are new. Live subtype and
binding uniqueness remain canonical binding authority; existing EMBODIES is
not displaced. Invalid/stale source, duplicate submission or late binding failure
cannot leave an extra Workspace through partial commit.

The new Workspace is canonical/native and sourceContextId is null. No Context,
H1/Big Picture, GroupScope, Planning, capabilities or Connections are manufactured.
It appears in canonical catalog/Library independently of placement. UI confirmation
is transient/non-replayed; success opens Workspace(workspaceId) once. Returning
refreshes bindings only and preserves unsaved semantic name/version state.
Both canonical types are admitted according to existing WorkspaceBinding rules,
not inferred from Orientation kind, Context role, title or visual ancestry.

HOST XML 66/66 tests in 8 classes (13 new), both separate Kotlin compile gates
PASS; Room 200 unchanged, no transport format or schema change. Native identity
and binding round-trip through canonical JSON Restore/Merge. Physical acceptance
remains pending.

### Explicit Workspace -> semantic bootstrap (R2D.5B)

The canonical Workspace action sheet can create a **new** Orientation or Aspect
and explicitly bind it to the initiating canonical workspaceId. The Workspace is
not converted, renamed, or semantically reclassified. This command is separate
from ordinary identity-only creation and from attaching an existing subject.

Semantic class, ordinary-authoring Orientation kind and binding type are explicit
user choices. DAY_THEME/ARC_QUEST compatibility kinds are not authoring choices.
All four existing binding types are available; only EMBODIES is primary, under
the existing zero/one embodied subject and Workspace uniqueness policy. A conflict
fails rather than replacing an existing binding or choosing another type.

Editable prefill is canonical Workspace title/name and Markdown description only.
No Context name fallback or role/type inference. Workspace tags are not copied;
the new subject's semantic tag membership remains independently authored.
Operational capabilities,
reminders, display/preset/scoring, Connections, H1 and Planning are not inputs.

WorkspaceSemanticBootstrapCoordinator uses one Room transaction around the normal
CanonicalOrientationRepository.createOrientation / CanonicalAspectRepository.create
and CanonicalWorkspaceBindingRepository.bind commands. It owns product composition,
not either family's invariants. Binding failure rolls back the subject, subtype,
initial assessment/revision and relation writes. Context-backed and standalone
owners use the same canonical Workspace admission; no Context writes/fabrication.

In-flight submission is guarded. Cancellation before confirmation creates nothing.
SavedState never replays creation: completed subject navigation is consumed once;
an interrupted confirmation warns on reopening to inspect bindings before a new
explicit creation. No persistent request ledger or automatic retry is introduced.
Success opens the exact Universal Semantic Editor. Catalog/Library and Properties
bindings update via existing canonical flows. Snapshot/JSON and canonical payload
Restore/Merge use ordinary identities/bindings, with no bootstrap-specific format.

H1 occurrences/containers (CURRENT after explicit V2 cutover), historical
GroupScope provenance, Planning, Context and capability-owned content remain
untouched. Room 200 unchanged. HOST evidence: 73/73 tests in 13 XML classes and
both separate Kotlin compile gates PASS; physical bootstrap acceptance pending.

### Canonical semantic source deletion (R2A.4)

`CanonicalSemanticEntityLifecycleCoordinator.tombstone(subjectId, subjectType,
expectedVersion, now)` is the domain entry, independent of Library/editor/H1 UI.
ManagedSubject, Orientation/Aspect and semantic relations remain
SEMANTIC_NOT_HIERARCHY. Placements/containers use CURRENT typed H1 authority after
the already accepted V2 cutover; this command changes no structural owner.
Historical GroupScope remains compatibility provenance only.

Exact generic admission:

| Target / kind | R2A.4 classification |
| --- | --- |
| Aspect without legacy mapping | SUPPORTED_BY_GENERIC_LIFECYCLE |
| INTENT | SUPPORTED_BY_GENERIC_LIFECYCLE, without legacy mapping |
| MILESTONE | SUPPORTED_BY_GENERIC_LIFECYCLE, without legacy mapping |
| ONGOING_STANDARD | SUPPORTED_BY_GENERIC_LIFECYCLE, without legacy mapping |
| OPPORTUNITY | SUPPORTED_BY_GENERIC_LIFECYCLE, without legacy mapping |
| MAIN_BEACON | SPECIALIZED_LIFECYCLE; generic entry blocked |
| MAIN_BEACON_GROUP | SPECIALIZED_LIFECYCLE; generic entry blocked |
| GOAL | SPECIALIZED_LIFECYCLE; generic entry blocked |
| DIRECTION | SPECIALIZED_LIFECYCLE; generic entry blocked |
| THEME | SPECIALIZED_LIFECYCLE; generic entry blocked |
| DAY_THEME | SPECIALIZED_LIFECYCLE compatibility vocabulary; blocked |
| QUEST | SPECIALIZED_LIFECYCLE; generic entry blocked |
| ARC_QUEST | SPECIALIZED_LIFECYCLE compatibility vocabulary; blocked |
| Unknown kind, incomplete subtype, legacy-mapped subject | BLOCKED / UNKNOWN for generic entry |

This is a conservative admission matrix, not a claim that every unmapped instance
of a specialized kind has a discovered owner. Such instances are deliberately
not generalized here. Any existing mapping (even tombstoned history) prevents
the generic command from guessing native provenance. Live PART_OF also rejects.

One outer Room transaction performs the following:

- retire all exact semantic-target PRIMARY and LINK placements through typed H1,
  leaves first when appearances of that same target are nested;
- retire membership, ordering and applicable sidecar state through H1, never UI;
- tombstone incident Orientation graph relations, Aspect refs and WorkspaceBindings
  through their existing authorities while endpoints are still live;
- tombstone ManagedSubject, increment version, set updatedAt and clear syncedAt;
- for Orientation, tombstone current assessment/scoring override, retaining subtype
  and immutable assessment revision evidence;
- for Aspect, reuse its canonical taxonomy-child-to-root promotion and lifecycle.

Unrelated Workspace identities, semantic targets, Context and containers survive.
Surviving unrelated occurrence/container children trigger the existing H1
non-leaf rejection policy, not automatic promotion/deletion. Live Planning
commitments are an explicit precondition failure; Planning must first retire its
own participation. Cancellation or late persistence/validation failure rolls back
the whole command. A current-version deleted replay is idempotent; stale subject
versions fail closed. No physical erasure or implicit lifecycle-value rewrite.

Live catalog/Library, including Unplaced, omit deleted subjects. Old semantic
editor routes return unavailable without recreation. The existing canonical
Orientation Global Search path excludes deleted identities via ProductRead;
Aspect is not a standalone Global Search result class in the current product.

Restore/Merge admission rejects live semantic relations/Aspect refs/bindings to
missing/deleted endpoints, both in incoming payload and the final merged state.
Older or equal-version live rows cannot resurrect semantic tombstones merely
because their wall clock is later. Exact current tombstone transport, existing
version-authoritative higher-version merge and exact-version ACK remain; no new
payload format. Actual Workspace selective-import closure and ingress are covered
alongside JSON Restore/Merge; no orphan live target is fabricated.

HOST evidence: 127/127 tests in 17 XML classes (22 new lifecycle/read/transport
tests), both separate Kotlin compile gates PASS, Room 200 unchanged. R2A.4 itself
included no product delete UI or physical deployment. R2F.2D now consumes this
command from typed Big Picture card actions; removing one occurrence remains a
completely different structural command.

R2F.2D also consumes CanonicalWorkspaceRepository.tombstone for canonical-owned,
non-System Workspace only. It checks the captured expected version and retires
all typed Workspace appearances/memberships/mixed order inside the existing
owned-content and binding lifecycle transaction. Bound semantic identities and
containers survive; unrelated live typed children reject the command. Context-backed
Workspace lifecycle is not generalized or delegated through a UI compatibility
fallback. Big Picture owns neither source deletion nor source cleanup policy.

## 5. Ownership, placement, and relation

The following are separate contracts:

- **ownership**: authoritative creation, mutation, deletion, versioning, sync;
- **placement**: where an identity appears, local order and local display state;
- **semantic relation**: why one identity is related to another;
- **commitment**: acceptance into a mission, iteration, or day;
- **execution/evidence**: what happened and when.

Deleting a placement never deletes its content. Deleting owned content
tombstones its placements and relations transactionally, without physically
cascading history. Restore must either restore valid links or report them.

Backlogs remain heterogeneous. Orientation placement is one supported content
type alongside Workspace links, documents, checklists, music, links, and other
existing content.

## 6. Relation vocabulary

Orientation relations use directed `from -> to` semantics:

| Type | Meaning | Ordered | Cycle rule |
| --- | --- | --- | --- |
| `PART_OF` | from is a component of to | yes, within parent | acyclic |
| `SUPPORTS` | from helps to | optional | no self-edge |
| `REALIZES` | realization of from advances to | optional | no self-edge |
| `DEPENDS_ON` | from requires to | optional | acyclic |
| `CONFLICTS_WITH` | symmetric conflict | no | canonical unordered pair |
| `PRECEDES` | from should precede to | optional | acyclic |
| `REFINES` | from is a more precise expression of to | optional | acyclic |
| `DERIVED_FROM` | from originated from to | no | acyclic |

All endpoints are Orientations. `PART_OF` permits multiple parents, while
Main Beacon Group membership is represented as ordered `MAIN_BEACON PART_OF
MAIN_BEACON_GROUP`. Existing specialized Beacon graph remains available during
compatibility and cutover.

Every relation uses a durable ID, versions, tombstones, endpoint indices, and
an idempotent uniqueness rule over the live logical edge. Deleting an endpoint
tombstones live relations; it does not delete the other endpoint. Unknown or
invalid edges are quarantined rather than rewritten.

Aspect hierarchy, Aspect membership, Workspace hierarchy, Workspace binding,
operational contribution, and Orientation relations remain separate tables and
validators.

## 7. Workspace capability contract

Each capability instance owns:

```text
id: UUID
workspaceId
capabilityType
instanceKey
order
state: ACTIVE | DISABLED | ARCHIVED
configurationVersion
configuration payload
createdAt / updatedAt / syncedAt / version / isDeleted
```

The schema permits multiple instances. The v1 registry sets `maxActive = 1`
for every initial type; later multiplicity changes therefore do not require an
identity redesign. `(workspaceId, capabilityType, instanceKey)` is the stable
logical identity, and `instanceKey = "default"` is used for migrated Context
capabilities.

Each capability module must declare:

- configuration schema and migration chain;
- owned collections and foreign-key boundary;
- enable, disable, archive, restore, and delete semantics;
- search/index contribution;
- backup and Android/Desktop sync ownership;
- navigation/view contribution;
- dependencies and incompatibilities;
- whether disabling hides data or changes runtime behavior.

Initial type mapping is:

The 2026-09-03 hard-removal decision supersedes the target status and the
earlier preservation semantics of `ARTIFACT` and context `JOURNAL` in this
original v1 mapping. They remain listed below only as historical v1 legacy
identifiers and are not current `WorkspaceCapabilityType` values. New code must
not activate, canonicalize, preserve, import, or recreate compatibility state
for them. Schema 165 is the destructive retirement boundary.

The corresponding entries in `domain-contract-v1.json` are retained as
historical evidence of the original v1 contract; they do not describe the
current runtime capability registry.

| Capability type | Current source |
| --- | --- |
| `BACKLOG` | `backlog` |
| `INBOX` | `inbox` |
| `INBOX_SORTING` | `inbox_sorting`; command-scoped dependencies on the selected target |
| `KEY_PROBLEMS` | `key_problems` |
| `DIRECTION` | `direction` |
| `ARTIFACT` | `artifact` |
| `DASHBOARD` | `dashboard` |
| `JOURNAL` | `journal_log` |
| `EXECUTION_LOG` | `log` |
| `CONNECTIONS` | `connections` and legacy `attachments` flag |
| `DOCUMENTS` | reserved explicit host for existing document collections |
| `NOTES` | reserved explicit host for note collections |
| `ATTACHMENTS` | reserved explicit host; not inferred from legacy flag |

Reserved types are not automatically activated during migration. Existing
documents, notes, and attachments remain reachable through compatibility until
their capability ownership is explicitly cut over.

Configuration is stored as versioned payload data but validated by a typed
shared-domain codec owned by the capability. An unknown configuration version
disables mutation and preserves raw data; it is never defaulted destructively.

## 8. Assessment history

Declared assessment and effective assessment are different projections.

Each accepted assessment mutation creates an immutable revision containing:

- Orientation ID and monotonically ordered revision identity;
- full declared axis snapshot, including origin/provenance;
- effective-from time and recorded-at time;
- actor/source (`USER`, `MIGRATION`, `IMPORT`, future automation);
- optional reason;
- version and tombstone metadata.

A current row points to or materializes the latest accepted revision for fast
queries. Revisions are not rewritten when the Orientation graph changes.

Contract v1 does not automatically inherit or aggregate assessment values.
Derived member distributions and effective projections are rebuildable query
results, never Group authority.

Historical reporting must declare one of:

```text
CURRENT_ASSESSMENT
AS_OF_ACTIVITY_TIME
AS_OF_PERIOD_END
```

Default interactive inventory filters use current assessment. Historical time
reflection defaults to `AS_OF_ACTIVITY_TIME` once revisions exist; until then
it must label its use of current values.

## 9. Contribution and time attribution

Operational entities remain specialized and link to Orientations with roles:

```text
ADVANCES
MAINTAINS
EXPLORES
PREVENTS
SUPPORTS
```

Mission, Day Task, Day Focus/Responsibility, and Activity each use a typed
domain relation rather than a universal unchecked `(type, id)` graph.

Time attribution modes are:

```text
INCLUSIVE     full duration appears under every related Orientation
ALLOCATED     duration is distributed by normalized positive weights
PRIMARY_ONLY full duration appears only under the one primary relation
```

An Activity total is counted once regardless of grouping. Within a grouping,
links are deduplicated by canonical Orientation ID. A direct Activity link
takes precedence over a derived path to the same Orientation. For allocated
mode, duplicate paths are collapsed before weights are normalized. Primary
mode requires exactly one valid primary relation; otherwise the record is
reported as unattributed rather than guessed.

Default grouped reflection is `INCLUSIVE` and must disclose that category sums
can exceed the unique total. Saved reports persist their selected mode.

## 10. Filter AST and saved views

Cross-client Filter AST version 1 has boolean nodes `ALL`, `ANY`, and `NOT`, and
typed predicates for:

- Orientation kind and lifecycle;
- ordered-axis comparison and range;
- explicit `UNSET` and `NOT_APPLICABLE` origin;
- Aspect membership with optional descendant inclusion;
- bounded relation traversal by relation type and direction;
- Workspace capability presence;
- planning/commitment coverage;
- contribution role and attribution mode;
- tag/category membership;
- text query;
- created, updated, target, and activity time windows.

The AST is data, not executable code. Relation traversal has an explicit depth
limit; v1 maximum is 8. Unknown node types, operators, axis values, or future
versions fail closed and preserve the raw saved view for later recovery.

A saved view owns:

```text
id, title, filterAstVersion, filterAst,
sort specification, grouping, visible fields,
createdAt, updatedAt, syncedAt, version, isDeleted
```

Android and Desktop compile the same semantics locally. Search indices and
materialized results are rebuildable and are not synchronized.

## 11. Legacy mapping specification

Legacy values remain stored until final cleanup even when a derived canonical
projection exists.

### Numeric Importance

For Goal or classified Context only when `scoringStatus = ASSESSED`:

```text
1..3   -> LOW
4..6   -> MEDIUM
7..9   -> HIGH
10..12 -> CRITICAL
```

The canonical origin is `DERIVED` with source field/value recorded. Values
outside the known 1..12 scale are not clamped; they remain `UNSET` with a
migration diagnostic. `NOT_ASSESSED` maps to `UNSET`.
`IMPOSSIBLE_TO_ASSESS` also maps to `UNSET` plus a distinct diagnostic, not to
`NOT_APPLICABLE`.

### Numeric Impact

For Goal or classified Context only when assessed:

```text
1  -> TINY
2  -> SMALL
3  -> MEDIUM
5  -> LARGE
8 or 13 -> TRANSFORMATIVE
```

Unknown values remain `UNSET` with diagnostics. The raw value survives.

### Day Task copies

DayTask `valueImportance` and `valueImpact` never create or overwrite canonical
assessment. A linked Orientation supplies effective values. For an unlinked
task, old values remain compatibility-only evidence; no Orientation is created
automatically.

### Relative size and other score fields

Goal `relativeSize`, effort, cost, risk, weights, raw/display score,
`parentValueImportance`, `impactOnParentGoal`, time cost, and financial cost are
preserved as legacy/specialized assessment data. None maps to Breadth,
Expected span, Impact, Attention tier, or operational priority in contract v1.

### Status mapping

When a legacy entity is projected as an Orientation:

```text
Goal ACTIVE    -> READY
Goal IN_WORK   -> ACTIVE
Goal PAUSED    -> PAUSED
Goal UNSURE    -> EXPLORING
Goal DONE      -> REALIZED
Goal CANCELED  -> DROPPED

Context NO_PLAN     -> READY
Context PLANNING    -> READY
Context IN_PROGRESS -> ACTIVE
Context COMPLETED   -> REALIZED
Context ON_HOLD     -> PAUSED
Context PAUSED      -> PAUSED

ArcQuest ACTIVE -> ACTIVE
ArcQuest PAUSED -> PAUSED
ArcQuest DONE   -> REALIZED
```

These are `DERIVED` compatibility mappings. Native status remains preserved
until its owner cuts over. Main Beacon readiness does not infer lifecycle.
Mission/Task priority does not infer Attention tier. Importance may provide a
one-time suggested Task priority during explicit task creation, as today, but
later changes never synchronize those fields.

Main Beacon `decisionImpact` is narrative and never maps to the categorical
Impact axis.

## 12. Context classification and migration boundary

There is no rule that converts every non-system Context into an Orientation.
Classification outcomes are:

```text
WORKSPACE_ONLY
ASPECT_AND_WORKSPACE
ORIENTATION_AND_WORKSPACE
WORKSPACE_WITH_RELATIONS
SYSTEM_OR_COMPATIBILITY_WORKSPACE
REVIEW_REQUIRED
```

Role, scoring, name, hierarchy, and attached data can produce suggestions but
not destructive automatic classification. In particular:

- role `aspect` suggests `ASPECT_AND_WORKSPACE`;
- role `main-beacon` plus an explicit Beacon link suggests an embodied Beacon
  Workspace;


`WORKSPACE_WITH_RELATIONS` is a classification/composition outcome, not an
atomic migration target. It means that the operational Workspace and its
semantic graph relations require separate explicit decisions. Context
retirement does not author arbitrary Workspace bindings as a side effect.

For a `main-beacon` Context, the role alone is insufficient to select a
canonical subject. An explicit Beacon link is required before embodiment. Main
Beacon semantic identity and Beacon-group `PART_OF` relations remain owned by
the Main Beacon Orientation bridge/cutover path. Workspace bindings remain
owned by the canonical Workspace-binding graph contract.
- project/direction roles or assessed Context values suggest possible
  `ORIENTATION_AND_WORKSPACE`;
- reserved system Context identities produce
  `SYSTEM_OR_COMPATIBILITY_WORKSPACE` and are not eligible for Context
  migration;
- non-system archive, capture, and technical operational roles may suggest
  `WORKSPACE_ONLY`;

Migration preview records the reason and confidence for every suggestion.
Unreviewed or ambiguous Contexts remain Workspaces using compatibility
adapters. Capabilities and navigation do not wait for semantic classification.


`SYSTEM_OR_COMPATIBILITY_WORKSPACE` and `REVIEW_REQUIRED` are classifier states,
not `ContextMigrationTarget` variants.

Reserved identities defined by `SystemContexts` are outside Context retirement.
The canonical migration command rejects them before target-specific work,
regardless of which target a caller supplies. System lifecycle policy therefore
cannot be bypassed through direct migration invocation.

`REVIEW_REQUIRED` is not a permanent intrinsic Context type. It means available
evidence is insufficient for automatic semantic interpretation. A non-system
Context remains a compatibility Workspace until explicit review. Once the user
chooses one of the accepted canonical targets, the classifier result imposes no
additional write restriction.

The accepted atomic migration vocabulary is complete for this contract:
new/existing Aspect with the existing Workspace, new/existing Orientation with
the existing Workspace, and Workspace-only retirement. Relation composition,
system compatibility, review state, and destructive Workspace lifecycle are not
additional migration targets.

The user-facing migration workflow is an orchestration surface over this
contract, not another authority. `classificationPreview()` may be displayed as
recommendation evidence, but it must not initialize an executable target
selection. The explicit migration choice starts unset and is written only by
the user's selection. New Orientation additionally requires explicit
`OrientationKind`; adoption requires an explicitly selected existing subject.

Read-only candidate projection may assist pickers, but final eligibility belongs
to the canonical migration command. UI confirmation does not weaken any
repository precondition, provenance rule, embodiment constraint, bottom-up
ordering rule, or system-Context exclusion.


There is intentionally no `ASPECT_ONLY` Context-classification or migration
outcome. An Aspect itself may exist without any Workspace, but legacy Context
migration begins with an already-existing operational Workspace. That Workspace
owns an independent lifecycle, hierarchy position, capability instances, and
capability-owned state.

Therefore Context-to-Aspect semantic retirement preserves the existing
Workspace rather than deleting it as part of migration. Removing the Workspace
after cutover, when explicitly desired, is a separate canonical Workspace
lifecycle action with its own destructive semantics. Aspect identity survives
that later Workspace deletion. Context migration must not infer Workspace
deletion from the user's semantic classification choice.

A reviewed `ORIENTATION_AND_WORKSPACE` Context may be explicitly cut over into
a **new** canonical Orientation while preserving its existing operational
Workspace. The caller explicitly selects `OrientationKind`; classifier output
is recommendation evidence only and never migration write authority.

The canonical Orientation owner creates the complete aggregate using the
deterministic Context semantic subject id and a kind-valid initial assessment.
Applicable axes begin `UNSET`; axes unavailable for that kind begin
`NOT_APPLICABLE`; `ONGOING_STANDARD` receives
`ExpectedSpan=ONGOING / DERIVED`.

The existing Context-backed Workspace keeps its id, operational parent edge and
capability-owned state. The canonical graph owner creates the single primary
`EMBODIES` binding and fails closed rather than displacing another live
embodiment. Workspace provenance becomes `CANONICAL_ONLY`, `sourceContextId`
is cleared, a durable live `CONTEXT -> subjectId` `LegacySubjectMapping` in
`CUT_OVER` state records semantic retirement, and the legacy Context is
tombstoned.

This target follows the same bottom-up leaf-only retirement invariant.
Identical retry is idempotent; conflicting semantic shape fails closed.
An already-existing canonical Orientation may also explicitly adopt one
otherwise-unowned legacy Context Workspace. This is adoption, not merge: the
existing ManagedSubject, Orientation node, lifecycle, current assessment and
revision history are not rewritten. Adoption requires a complete active
Orientation aggregate, no prior `LegacySubjectMapping` reservation including
tombstones, and no conflicting live `EMBODIES` edge. The existing Context
Workspace must not embody another subject. The canonical graph owner creates or
reuses the exact primary embodiment without displacement; Workspace promotion,
`CONTEXT/CUT_OVER` provenance and Context tombstoning follow the same bottom-up
retirement contract.


`WORKSPACE_ONLY` is a non-semantic retirement target. It preserves the existing
Context-backed Workspace as the canonical operational identity and creates no
ManagedSubject, Orientation, Aspect, `LegacySubjectMapping`, or
`WorkspaceBinding`.

Fresh cutover requires a live leaf Context and the same live Workspace in exact
`CONTEXT_BACKED` provenance with `sourceContextId` equal to the Context id. Any
existing CONTEXT legacy-subject mapping or live `EMBODIES` binding fails closed.
The Workspace keeps its id, operational parent edge, capability instances and
capability-owned state; provenance becomes `CANONICAL_ONLY`,
`sourceContextId` is cleared, and the Context is tombstoned.

No separate retirement ledger is required. The durable completed-state evidence
is the combined persisted shape: tombstoned Context, same live
`CANONICAL_ONLY` Workspace with no `sourceContextId`, and no CONTEXT
`LegacySubjectMapping`.

A live `EMBODIES` edge is a **fresh-cutover precondition**, not permanent
retirement evidence. Workspace-only cutover rejects a pre-existing embodiment
because migration must not silently reinterpret semantic ownership. After
cutover, the canonical Workspace may independently acquire `EMBODIES`,
`REALIZES`, `SUPPORTS`, or `MONITORS` bindings. Such later graph evolution does
not invalidate the already-completed Context retirement.

Workspace bootstrap cannot reverse this ownership transition: a legacy Context
projection may only propose `CONTEXT_BACKED`, and an existing canonical-only
Workspace is quarantined as an id collision rather than overwritten. Exact
Workspace-only retry is idempotent; semantic cutovers are not eligible because
they retain their CONTEXT-to-subject CUT_OVER mapping.

## 13. Theme ownership and daily overrides

`ThemeDefinition` maps one-to-one to the semantic `DAY_THEME` Orientation.
`DayTheme` remains the daily assignment.

Definition/Orientation owns:

- title, description, reusable color/icon metadata;
- Importance, Impact, Breadth, Commitment, Confidence;
- semantic lifecycle/history.

Daily assignment owns:

- Day Plan relation;
- budget percent, order, active state;
- optional day-specific Attention tier;
- future daily note, if explicitly added.

Daily assignment cannot override title, description, Importance, Impact,
Breadth, Commitment, or Confidence in contract v1.

## 14. Sync and deletion defaults for new collections

New canonical collections begin as Android read-only on Desktop during shadow
projection. They cannot enter Desktop push until both clients implement shared
validation, merge, tombstone, acknowledgement, and round-trip tests.

After cutover, subject, Orientation, Aspect, assessment, relation, Workspace
binding, capability-instance, and saved-view collections are intended to be
bidirectional. The exact transition is a per-phase policy change, not implied
by this document.

All new rows have version, timestamps, tombstone, and anti-resurrection rules
from their first schema. Physical deletion is reserved for local rebuildable
indices or explicit compaction after acknowledgement guarantees.

## 15. Explicitly rejected shortcuts

- making Workspace a ManagedSubject subtype;
- turning every Context, Mission, Day Task, or Activity into an Orientation;
- using one polymorphic relation table for the whole application;
- treating `relativeSize` as Breadth or Expected span;
- treating Beacon `decisionImpact` text as Impact;
- mapping `IMPOSSIBLE_TO_ASSESS` to `NOT_APPLICABLE`;
- deriving Group assessment from members as authority;
- allowing multiple primary embodied Workspaces;
- copying subject title into Workspace as a second editable authority;
- silently ignoring unknown Filter AST nodes or enum codes;
- auto-promoting tags into Aspects;
- changing current UI as part of domain migration without separate approval.

## Accepted decision boundary

Acceptance of this contract approves the semantic vocabulary and constraints,
not unrelated implementation or UI changes. It is the input to Phase 2 shared
read-only contracts and compatibility projections.

The most consequential accepted choices are:

1. categorical axis value sets and numeric mapping thresholds;
2. one-parent Aspect hierarchy in v1;
3. exactly one primary embodied Workspace per subject and per Workspace;
4. no blanket Context-to-Orientation conversion;
5. Day Theme applicability and permitted daily override;
6. default inclusive time attribution;
7. deterministic UUIDv5 mapping for every legacy semantic identity.
