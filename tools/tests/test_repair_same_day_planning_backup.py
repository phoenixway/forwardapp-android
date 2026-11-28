import copy
from datetime import date
import io
import json
from pathlib import Path
import runpy
import subprocess
import sys
import tempfile
import unittest
from uuid import uuid5
from zoneinfo import ZoneInfo


TOOL = Path(__file__).resolve().parents[1] / 'repair-same-day-planning-backup.py'
MODULE = runpy.run_path(str(TOOL))
ALIGN = MODULE['align_backups']
ERROR = MODULE['RecoveryError']
DAY = date(2026, 11, 27)
ZONE = ZoneInfo('Europe/Kyiv')
MIDNIGHT = 1795730400000
ANCHOR_ID = '11111111-1111-4111-8111-111111111111'
PEER_ID = '22222222-2222-4222-8222-222222222222'


def snapshot(day_id, clock, scope_version):
    def row(**values):
        return dict(version=1, createdAt=clock, updatedAt=clock, isDeleted=False, **values)

    task = row(id='shared-task', dayPlanId=day_id, title='Keep this task', recurrence={
        'seriesId': 'task-series', 'occurrenceDayKey': DAY.isoformat(), 'sourceSeriesVersion': 1,
    })
    focus = row(id='shared-focus', dayPlanId=day_id, title='Keep this priority')
    scope = row(id=day_id, kind='DAY', startsAt=MIDNIGHT)
    scope['version'] = scope_version
    return {
        'backupSchemaVersion': 2,
        'settings': {'setting': clock},
        'snapshotBundle': {
            'planningFormatVersion': 5,
            'dayPlans': [row(id=day_id, date=MIDNIGHT)],
            'planningScopes': [scope],
            'dayPlanningScopeExtensions': [row(scopeId=day_id, calendarDayStartMillis=MIDNIGHT)],
            'dayTasks': [task, row(id='other-task', dayPlanId='different-plan', title='Independent edit')],
            'dayFocusItems': [focus],
            'recurringSeries': [row(id='task-series', kind='TASK')],
            'managedSubjects': [row(id='source', subjectType='ORIENTATION')],
            'orientations': [{'subjectId': 'source', 'kind': 'INTENT'}],
            'planningCommitments': [row(id='commitment', scopeId=day_id, role='DAY.TASK',
                                        orientationId='source', provenanceSourceType='DAY_TASK',
                                        provenanceSourceId='shared-task')],
            'planningReferences': [row(id='reference', ownerKind='DAY_TASK', ownerId='shared-task', role='SOURCE',
                                       targetKind='ORIENTATION', targetId='source')],
        },
    }


def add_anchor_theme(anchor):
    bundle = anchor['snapshotBundle']
    theme = f'day_theme:36:{ANCHOR_ID}:5:theme'
    commitment = str(uuid5(MODULE['DAY_THEME_COMMITMENT_NAMESPACE'], f'DAY_THEME_COMMITMENT:{theme}'))
    bundle['dayThemes'] = [{'id': theme, 'dayPlanId': ANCHOR_ID, 'themeId': 'theme'}]
    bundle['themeDefinitions'] = [{'id': 'theme'}]
    bundle['managedSubjects'].append({'id': 'theme-orientation', 'subjectType': 'ORIENTATION'})
    bundle['orientations'].append({'subjectId': 'theme-orientation', 'kind': 'THEME'})
    bundle['legacySubjectMappings'] = [{'id': 'mapping', 'sourceType': 'THEME_DEFINITION', 'sourceId': 'theme',
                                      'state': 'CUT_OVER', 'subjectId': 'theme-orientation'}]
    bundle['planningCommitments'] += [
        {'id': 'previous', 'scopeId': 'different-plan', 'role': 'DAY.THEME', 'orientationId': 'theme-orientation'},
        {'id': commitment, 'scopeId': ANCHOR_ID, 'role': 'DAY.THEME', 'orientationId': 'theme-orientation',
         'provenanceSourceType': 'PLANNING_COMMITMENT', 'provenanceSourceId': 'previous'},
    ]


class RepairSameDayPlanningBackupTest(unittest.TestCase):
    def setUp(self):
        self.anchor = snapshot(ANCHOR_ID, 100, 4)
        self.peer = snapshot(PEER_ID, 200, 1)

    def align(self):
        return ALIGN(self.anchor, self.peer, DAY, ZONE)

    def test_aligns_only_proven_rows_and_retains_higher_anchor_version(self):
        add_anchor_theme(self.anchor)
        anchor_before, peer_before = copy.deepcopy(self.anchor), copy.deepcopy(self.peer)
        result, counts = self.align()

        anchor, peer, output = [backup['snapshotBundle'] for backup in (self.anchor, self.peer, result)]
        self.assertEqual(output['planningScopes'], anchor['planningScopes'])
        self.assertEqual(output['planningScopes'][0]['version'], 4)
        self.assertEqual(output['dayTasks'], [anchor['dayTasks'][0], peer['dayTasks'][1]])
        self.assertEqual(output['planningReferences'], anchor['planningReferences'])
        self.assertEqual(output['planningCommitments'], anchor['planningCommitments'][:1])
        self.assertEqual(result['settings'], self.peer['settings'])
        self.assertNotIn(PEER_ID, json.dumps(result))
        self.assertEqual(counts['anchorOnlyDayThemes'], 1)
        self.assertEqual(self.anchor, anchor_before)
        self.assertEqual(self.peer, peer_before)

    def test_rejects_divergent_content_and_newer_peer_revision(self):
        task = self.peer['snapshotBundle']['dayTasks'][0]
        task['title'] = 'Unsynced peer edit'
        with self.assertRaisesRegex(ERROR, 'Incompatible edits in dayTasks'):
            self.align()
        task['title'] = 'Keep this task'
        task['version'] = 2
        with self.assertRaisesRegex(ERROR, 'Newer or invalid peer version in dayTasks'):
            self.align()

    def test_rejects_unattested_midnight_and_second_plan_with_same_local_date(self):
        with self.assertRaisesRegex(ERROR, 'differs from the attested local midnight'):
            ALIGN(self.anchor, self.peer, DAY, ZoneInfo('Asia/Tbilisi'))
        self.peer['snapshotBundle']['dayPlans'].append({
            'id': '33333333-3333-4333-8333-333333333333', 'date': MIDNIGHT + 3_600_000,
        })
        with self.assertRaisesRegex(ERROR, 'exactly one live DayPlan'):
            self.align()

    def test_rejects_unsupported_reference_even_outside_known_families(self):
        self.peer['snapshotBundle']['futureFamily'] = [{'nested': {f'wrapped:{PEER_ID}': 'value'}}]
        with self.assertRaisesRegex(ERROR, 'unsupported reference'):
            self.align()

    def test_rejects_duplicate_ids_and_unresolved_sources(self):
        task = self.peer['snapshotBundle']['dayTasks'][0]
        self.peer['snapshotBundle']['dayTasks'].append(dict(task))
        with self.assertRaisesRegex(ERROR, 'Duplicate identity in dayTasks'):
            self.align()
        self.peer['snapshotBundle']['dayTasks'].pop()
        self.peer['snapshotBundle']['planningReferences'][0]['targetId'] = 'missing'
        with self.assertRaisesRegex(ERROR, 'Unresolved Planning reference target'):
            self.align()

    def test_rejects_duplicate_json_fields(self):
        with self.assertRaisesRegex(ERROR, 'Duplicate JSON object key'):
            MODULE['load_backup'](io.StringIO('{"value":1,"value":2}'))

    def test_cli_never_overwrites_existing_output(self):
        with tempfile.TemporaryDirectory() as temporary:
            folder = Path(temporary)
            anchor, peer, output = [folder / name for name in ['anchor.json', 'peer.json', 'output.json']]
            anchor.write_text(json.dumps(self.anchor))
            peer.write_text(json.dumps(self.peer))
            output.write_text('preserve')
            command = [sys.executable, str(TOOL), '--anchor', str(anchor), '--peer', str(peer),
                       '--output', str(output), '--date', DAY.isoformat(), '--timezone', ZONE.key]

            result = subprocess.run(command, capture_output=True, text=True, check=False)

            self.assertEqual(result.returncode, 1)
            self.assertEqual(output.read_text(), 'preserve')


if __name__ == '__main__':
    unittest.main()
