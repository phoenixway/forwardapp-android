#!/usr/bin/env python3
"""Align a duplicate canonical DayPlan in an offline peer backup with a proven anchor."""

import argparse
from datetime import date, datetime, time, timedelta, timezone
import json
from pathlib import Path
from uuid import UUID, uuid5
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError


IDENTITIES = {
    'dayPlans': 'id',
    'planningScopes': 'id',
    'dayPlanningScopeExtensions': 'scopeId',
    'dayTasks': 'id',
    'dayFocusItems': 'id',
    'planningCommitments': 'id',
    'planningReferences': 'id',
    'planningCommitmentRelations': 'id',
    'dayThemes': 'id',
    'dayThemeAssignmentDocuments': 'dayPlanId',
    'themeDefinitions': 'id',
    'recurringSeries': 'id',
    'orientations': 'subjectId',
    'managedSubjects': 'id',
    'legacySubjectMappings': 'id',
}

PARTICIPATION = {
    'dayPlans': 'id',
    'planningScopes': 'id',
    'dayPlanningScopeExtensions': 'scopeId',
    'dayTasks': 'dayPlanId',
    'dayFocusItems': 'dayPlanId',
    'planningCommitments': 'scopeId',
}

CLOCK_FIELDS = {'createdAt', 'updatedAt', 'syncedAt', 'version'}
DAY_THEME_COMMITMENT_NAMESPACE = UUID('f20bbf38-f08b-5ea3-b754-c123445f5a24')


class RecoveryError(ValueError):
    pass


def require(condition, message):
    if not condition:
        raise RecoveryError(message)


def load_backup(stream):
    def unique_object(pairs):
        result = dict(pairs)
        require(len(result) == len(pairs), 'Duplicate JSON object key')
        return result

    return json.load(stream, object_pairs_hook=unique_object)


def index(bundle, family):
    key = IDENTITIES[family]
    rows = bundle.get(family, [])
    require(isinstance(rows, list), f'Invalid {family} collection')
    result = {}
    for row in rows:
        require(isinstance(row, dict), f'Malformed {family} row')
        identifier = row.get(key)
        require(isinstance(identifier, str) and identifier, f'Missing {family} identity')
        require(identifier not in result, f'Duplicate identity in {family}')
        result[identifier] = row
    return result


def exact_calendar_window(day, zone):
    midnight = datetime.combine(day, time.min, zone)
    next_day = datetime.combine(day + timedelta(days=1), time.min, zone)
    require(midnight.astimezone(timezone.utc).astimezone(zone) == midnight,
            'Local midnight does not exist in the attested zone')
    return int(midnight.timestamp() * 1000), int(next_day.timestamp() * 1000)


def load_graph(backup, day, zone):
    require(backup.get('backupSchemaVersion') == 2, 'Expected a full v2 Android backup')
    bundle = backup.get('snapshotBundle')
    require(isinstance(bundle, dict) and bundle.get('planningFormatVersion') == 5,
            'Expected canonical Planning v5')
    indexes = {family: index(bundle, family) for family in IDENTITIES}
    start, end = exact_calendar_window(day, zone)
    candidates = [r for r in indexes['dayPlans'].values()
                  if type(r.get('date')) is int and start <= r['date'] < end and not r.get('isDeleted')]
    require(len(candidates) == 1, 'Each backup must have exactly one live DayPlan on the attested date')
    plan = candidates[0]
    day_id = plan['id']
    require(str(UUID(day_id)) == day_id, 'Expected a canonical DayPlan UUID')
    require(plan['date'] == start, 'DayPlan timestamp differs from the attested local midnight')
    graph = {
        family: {identity: row for identity, row in indexes[family].items() if row.get(field) == day_id}
        for family, field in PARTICIPATION.items()
    }
    scope = graph['planningScopes'].get(day_id)
    extension = graph['dayPlanningScopeExtensions'].get(day_id)
    require(scope and scope.get('kind') == 'DAY' and not scope.get('isDeleted') and scope.get('startsAt') == start,
            'Missing or incompatible canonical DAY scope')
    require(extension and not extension.get('isDeleted') and extension.get('calendarDayStartMillis') == start,
            'Missing or incompatible canonical day extension')
    recorded_calendar = extension.get('localDate'), extension.get('timeZoneId')
    require(recorded_calendar in {(None, None), (day.isoformat(), zone.key)},
            'Recorded calendar identity conflicts with the operator attestation')
    require(sum(
        row.get('calendarDayStartMillis') == start and not row.get('isDeleted')
        for row in indexes['dayPlanningScopeExtensions'].values()
    ) == 1, 'Multiple live DAY scope extensions at the attested midnight')

    owners = {'DAY_TASK': graph['dayTasks'], 'DAY_FOCUS': graph['dayFocusItems']}
    owner_ids = set(owners['DAY_TASK']) | set(owners['DAY_FOCUS'])
    require(len(owner_ids) == len(owners['DAY_TASK']) + len(owners['DAY_FOCUS']), 'Ambiguous Day owner identity')
    for family, kind in (('dayTasks', 'TASK'), ('dayFocusItems', 'FOCUS')):
        keys = set()
        for row in graph[family].values():
            provenance = row.get('recurrence')
            if provenance is None:
                continue
            series_id = provenance.get('seriesId')
            series = indexes['recurringSeries'].get(series_id)
            kinds = {kind} if kind == 'TASK' else {'FOCUS', 'RESPONSIBILITY'}
            require(series and series.get('kind') in kinds and provenance.get('occurrenceDayKey') == day.isoformat(),
                    'Unresolved recurrence identity on the selected day')
            require(series_id not in keys, 'Duplicate logical occurrence on the selected day')
            keys.add(series_id)
    for row in graph['dayTasks'].values():
        require(set(row.get('realizedDayFocusItemIds') or []) <= set(graph['dayFocusItems']),
                'Task realization refers to a priority outside the selected DayPlan')

    def has_orientation(identity):
        return identity in indexes['orientations'] and indexes['managedSubjects'].get(identity, {}).get('subjectType') == 'ORIENTATION'

    graph['planningReferences'] = {}
    sources = {}
    for identity, row in indexes['planningReferences'].items():
        if row.get('ownerId') not in owner_ids:
            continue
        require(row.get('ownerId') in owners.get(row.get('ownerKind'), {}), 'Unsupported Planning reference owner')
        require(row.get('targetKind') == 'ORIENTATION' and has_orientation(row.get('targetId')),
                'Unresolved Planning reference target')
        if row.get('role') == 'SOURCE' and not row.get('isDeleted'):
            owner = row['ownerId']
            require(owner not in sources, 'Multiple live SOURCE references for one owner')
            sources[owner] = row['targetId']
        graph['planningReferences'][identity] = row

    themes = {identity: row for identity, row in indexes['dayThemes'].items() if row.get('dayPlanId') == day_id}
    theme_roles = {}
    for row in graph['planningCommitments'].values():
        orientation = row.get('orientationId')
        require(has_orientation(orientation), 'Unresolved Planning commitment Orientation')
        provenance = row.get('provenanceSourceId')
        if row.get('role') == 'DAY.TASK':
            require(row.get('provenanceSourceType') == 'DAY_TASK' and provenance in graph['dayTasks'],
                    'Unresolved DAY.TASK commitment owner')
            require(row.get('isDeleted') or sources.get(provenance) == orientation,
                    'DAY.TASK commitment and SOURCE reference disagree')
        elif row.get('role') == 'DAY.THEME':
            require(row.get('provenanceSourceType') == 'PLANNING_COMMITMENT'
                    and provenance in indexes['planningCommitments'] and orientation not in theme_roles,
                    'Unresolved or duplicate DAY.THEME participation')
            theme_roles[orientation] = row
        else:
            raise RecoveryError('Unsupported commitment role on the selected day')
    require(len(themes) == len(theme_roles), 'Day Theme rows and DAY.THEME participations differ in count')
    for identifier, row in themes.items():
        theme_id = row.get('themeId')
        require(theme_id in indexes['themeDefinitions'], 'Unresolved ThemeDefinition on the selected day')
        units = len(theme_id.encode('utf-16-le')) // 2
        require(identifier == f'day_theme:{len(day_id)}:{day_id}:{units}:{theme_id}', 'Noncanonical Day Theme identity')
        mappings = [item for item in indexes['legacySubjectMappings'].values()
                    if item.get('sourceType') == 'THEME_DEFINITION' and item.get('sourceId') == theme_id
                    and item.get('state') == 'CUT_OVER' and not item.get('isDeleted')]
        require(len(mappings) == 1, 'Unresolved ThemeDefinition Orientation mapping')
        orientation = mappings[0].get('subjectId')
        role = theme_roles.get(orientation, {})
        expected_id = str(uuid5(DAY_THEME_COMMITMENT_NAMESPACE, f'DAY_THEME_COMMITMENT:{identifier}'))
        require(role.get('id') == expected_id and indexes['orientations'][orientation].get('kind') == 'THEME',
                'Day Theme and DAY.THEME role identities disagree')
    document = indexes['dayThemeAssignmentDocuments'].get(day_id)
    if document:
        require(all(row.get('entityId') in owner_ids and set(row.get('dayThemeIds') or []) <= themes.keys()
                    for row in document.get('assignments', [])), 'Unresolved Day Theme assignment')
    related = {identity: row for identity, row in indexes['planningCommitmentRelations'].items()
               if row.get('sourceCommitmentId') in graph['planningCommitments']
               or row.get('targetCommitmentId') in graph['planningCommitments']}
    require(not related, 'Selected commitment composition requires a separate repair')
    return bundle, indexes, day_id, graph, themes, document


def references_identifier(value, identifier):
    if isinstance(value, dict):
        return any(identifier in key or references_identifier(item, identifier) for key, item in value.items())
    if isinstance(value, list):
        return any(references_identifier(item, identifier) for item in value)
    return isinstance(value, str) and identifier in value


def align_backups(anchor, peer, day, zone):
    _, anchor_index, anchor_id, anchor_graph, anchor_themes, _ = load_graph(anchor, day, zone)
    peer_bundle, peer_index, peer_id, peer_graph, peer_themes, peer_assignments = load_graph(peer, day, zone)
    require(anchor_id != peer_id and anchor_id not in peer_index['dayPlans']
            and anchor_id not in peer_index['planningScopes']
            and anchor_id not in peer_index['dayPlanningScopeExtensions']
            and peer_id not in anchor_index['dayPlans'] and peer_id not in anchor_index['planningScopes'],
            'Plan identities already match or collide with another row')
    require(not peer_themes and not peer_assignments, 'Peer Day Theme graph needs a separate repair')
    require(peer_graph['dayTasks'] or peer_graph['dayFocusItems'], 'No shared stable occurrence identity to prove this repair')
    output_bundle = dict(peer_bundle)
    counts = {}
    for family, peer_rows in peer_graph.items():
        field = PARTICIPATION.get(family)
        old_keys = set(peer_rows)
        new_keys = {anchor_id if identity == peer_id else identity for identity in old_keys}
        anchor_rows = anchor_graph[family]
        extras = anchor_rows.keys() - new_keys
        require(new_keys <= anchor_rows.keys() and (not extras or family == 'planningCommitments'
                and all(anchor_rows[key].get('role') == 'DAY.THEME' and key not in peer_index[family] for key in extras)),
                f'Different participation in {family}')
        updated = {}
        for identity, peer_row in peer_rows.items():
            anchor_row = anchor_rows[anchor_id if identity == peer_id else identity]
            normalized = {**peer_row, field: anchor_id} if field is not None else peer_row
            require({k: v for k, v in normalized.items() if k not in CLOCK_FIELDS}
                    == {k: v for k, v in anchor_row.items() if k not in CLOCK_FIELDS},
                    f'Incompatible edits in {family}')
            require(type(anchor_row.get('version')) is int and type(peer_row.get('version')) is int
                    and anchor_row['version'] >= peer_row['version'], f'Newer or invalid peer version in {family}')
            updated[identity] = anchor_row
        if updated:
            output_bundle[family] = [updated.get(row[IDENTITIES[family]], row) for row in peer_bundle[family]]
        counts[family] = len(updated)

    result = {**peer, 'snapshotBundle': output_bundle}
    require(not references_identifier(result, peer_id), 'The peer DayPlan still has an unsupported reference')
    _, _, repaired_id, _, _, _ = load_graph(result, day, zone)
    require(repaired_id == anchor_id, 'Recovery validation selected an unexpected DayPlan')
    counts['anchorOnlyDayThemes'] = len(anchor_themes)
    return result, counts


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--anchor', type=Path, required=True, help='Backup with the DayPlan UUID to retain')
    parser.add_argument('--peer', type=Path, required=True, help='Peer backup containing the duplicate DayPlan')
    parser.add_argument('--output', type=Path, required=True, help='New recovery backup path')
    parser.add_argument('--date', type=date.fromisoformat, required=True, help='Attested local date on both devices')
    parser.add_argument('--timezone', required=True, help='Attested IANA timezone on both devices')
    args = parser.parse_args()
    try:
        with args.anchor.open(encoding='utf-8') as a, args.peer.open(encoding='utf-8') as b:
            result, counts = align_backups(load_backup(a), load_backup(b), args.date, ZoneInfo(args.timezone))
        with args.output.open('x', encoding='utf-8') as output:
            try:
                json.dump(result, output, ensure_ascii=False, separators=(',', ':'))
                output.write('\n')
                output.flush()
            except BaseException:
                args.output.unlink()
                raise
    except (RecoveryError, ZoneInfoNotFoundError, OSError, ValueError, TypeError, AttributeError) as error:
        parser.exit(1, f'Recovery stopped. {error}\n')
    print('Recovery backup written. ' + ', '.join(f'{key}={value}' for key, value in counts.items()))


if __name__ == '__main__':
    main()
