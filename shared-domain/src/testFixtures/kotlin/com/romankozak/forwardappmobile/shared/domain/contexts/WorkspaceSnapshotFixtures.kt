package com.romankozak.forwardappmobile.shared.domain.contexts

object WorkspaceSnapshotFixtures {
    const val DESKTOP_MINIMAL_JSON =
        """
        {
          "contexts": [
            {
              "id": "core",
              "name": "Core",
              "description": "Root",
              "parentId": null,
              "status": "InProgress",
              "defaultView": "Dashboard",
              "score": 10,
              "isCompleted": false
            }
          ],
          "backlogItems": []
        }
        """

    const val DESKTOP_TREE_JSON =
        """
        {
          "contexts": [
            {
              "id": "root",
              "name": "Root",
              "description": "Top level",
              "parentId": null,
              "status": "InProgress",
              "defaultView": "Dashboard",
              "score": 10,
              "isCompleted": false
            },
            {
              "id": "project",
              "name": "Project",
              "description": "Parent branch",
              "parentId": "root",
              "status": "Planning",
              "defaultView": "Backlog",
              "score": 5,
              "isCompleted": false
            },
            {
              "id": "child",
              "name": "Child",
              "description": "Nested branch",
              "parentId": "project",
              "status": "NoPlan",
              "defaultView": "Backlog",
              "score": 3,
              "isCompleted": false
            }
          ],
          "backlogItems": [
            {
              "id": "project-item",
              "contextId": "project",
              "title": "Parent backlog",
              "details": "Should be removed",
              "kind": "Task",
              "priority": "Medium",
              "isDone": false
            },
            {
              "id": "child-item",
              "contextId": "child",
              "title": "Child backlog",
              "details": "Should be removed too",
              "kind": "Task",
              "priority": "High",
              "isDone": false
            }
          ]
        }
        """

    const val ANDROID_SNAPSHOT_BUNDLE_JSON =
        """
        {
          "backupSchemaVersion": 2,
          "exportedAt": 1767225600000,
          "snapshotBundle": {
            "snapshotVersion": 2,
            "exportedAt": 1767225600000,
            "contexts": [
              {
                "id": "root",
                "name": "Android Root",
                "description": "Restored from Android",
                "parentId": null,
                "createdAt": 1,
                "updatedAt": 2,
                "isExpanded": true,
                "isDeleted": false,
                "version": 1,
                "tags": [],
                "relatedLinks": [],
                "order": 1,
                "isAttachmentsExpanded": false,
                "defaultViewModeName": "DASHBOARD",
                "isCompleted": false,
                "isContextManagementEnabled": true,
                "contextStatus": "IN_PROGRESS",
                "contextStatusText": null,
                "contextLogLevel": "NORMAL",
                "totalTimeSpentMinutes": 0,
                "valueImportance": 0,
                "valueImpact": 0,
                "effort": 0,
                "cost": 0,
                "risk": 0,
                "weightEffort": 1.0,
                "weightCost": 1.0,
                "weightRisk": 1.0,
                "rawScore": 0.0,
                "displayScore": 12.0,
                "scoringStatus": "ASSESSED",
                "showCheckboxes": false,
                "roleCode": null
              },
              {
                "id": "project",
                "name": "Android Project",
                "description": "Imported branch",
                "parentId": "root",
                "createdAt": 1,
                "updatedAt": 2,
                "isExpanded": true,
                "isDeleted": false,
                "version": 1,
                "tags": [],
                "relatedLinks": [],
                "order": 2,
                "isAttachmentsExpanded": false,
                "defaultViewModeName": "BACKLOG",
                "isCompleted": false,
                "isContextManagementEnabled": true,
                "contextStatus": "PLANNING",
                "contextStatusText": null,
                "contextLogLevel": "NORMAL",
                "totalTimeSpentMinutes": 0,
                "valueImportance": 0,
                "valueImpact": 0,
                "effort": 0,
                "cost": 0,
                "risk": 0,
                "weightEffort": 1.0,
                "weightCost": 1.0,
                "weightRisk": 1.0,
                "rawScore": 0.0,
                "displayScore": 7.0,
                "scoringStatus": "ASSESSED",
                "showCheckboxes": false,
                "roleCode": null
              }
            ],
            "goals": [
              {
                "id": "goal-1",
                "text": "Ship desktop importer",
                "description": "Recover Android state into desktop",
                "isCompleted": true,
                "goalStatus": "DONE",
                "createdAt": 1,
                "updatedAt": 2,
                "version": 1,
                "isDeleted": false,
                "tags": [],
                "scoringStatus": "ASSESSED",
                "valueImportance": 0,
                "valueImpact": 0,
                "effort": 0,
                "cost": 0,
                "risk": 0,
                "weightEffort": 1.0,
                "weightCost": 1.0,
                "weightRisk": 1.0,
                "rawScore": 0.0,
                "displayScore": 0.0,
                "relativeSize": 0,
                "parentValueImportance": null,
                "impactOnParentGoal": null,
                "timeCost": null,
                "financialCost": null
              }
            ],
            "documents": [
              {
                "id": "doc-1",
                "name": "Architecture Notes",
                "contextId": "project",
                "content": "Desktop migration plan",
                "createdAt": 1,
                "updatedAt": 2,
                "version": 1,
                "isDeleted": false
              }
            ],
            "backlogItems": [
              {
                "id": "item-1",
                "contextId": "project",
                "itemType": "GOAL",
                "entityId": "goal-1",
                "order": 10,
                "updatedAt": 2,
                "version": 1,
                "isDeleted": false
              },
              {
                "id": "item-2",
                "contextId": "project",
                "itemType": "NOTE_DOCUMENT",
                "entityId": "doc-1",
                "order": 20,
                "updatedAt": 2,
                "version": 1,
                "isDeleted": false
              }
            ],
            "backlogOrders": [
              {
                "id": "order-1",
                "listId": "project",
                "itemId": "item-1",
                "order": 1,
                "orderVersion": 1,
                "updatedAt": 2,
                "isDeleted": false
              },
              {
                "id": "order-2",
                "listId": "project",
                "itemId": "item-2",
                "order": 2,
                "orderVersion": 1,
                "updatedAt": 2,
                "isDeleted": false
              }
            ]
          }
        }
        """

    const val ANDROID_LEGACY_DATABASE_JSON =
        """
        {
          "backupSchemaVersion": 1,
          "exportedAt": 1767225600000,
          "database": {
            "projects": [
              {
                "id": "root",
                "name": "Legacy Root",
                "description": "Legacy import",
                "parentId": null,
                "createdAt": 1,
                "updatedAt": 2,
                "isDeleted": false,
                "version": 1,
                "order": 1,
                "defaultViewModeName": "DASHBOARD",
                "isCompleted": false,
                "contextStatus": "IN_PROGRESS",
                "displayScore": 3
              },
              {
                "id": "legacy-project",
                "name": "Legacy Project",
                "description": "Migrated from old backup",
                "parentId": "root",
                "createdAt": 1,
                "updatedAt": 2,
                "isDeleted": false,
                "version": 1,
                "order": 2,
                "defaultViewModeName": "BACKLOG",
                "isCompleted": false,
                "contextStatus": "PLANNING",
                "displayScore": 8
              }
            ],
            "goals": [
              {
                "id": "goal-legacy",
                "text": "Recover legacy backup",
                "description": "Legacy Android import",
                "completed": false,
                "updatedAt": 2,
                "isDeleted": false
              }
            ],
            "documents": [
              {
                "id": "doc-legacy",
                "contextId": "legacy-project",
                "name": "Legacy Doc",
                "content": "Legacy content",
                "updatedAt": 2,
                "isDeleted": false
              }
            ],
            "listItems": [
              {
                "id": "legacy-item-1",
                "contextId": "legacy-project",
                "itemType": "GOAL",
                "entityId": "goal-legacy",
                "order": 10,
                "updatedAt": 2,
                "isDeleted": false
              },
              {
                "id": "legacy-item-2",
                "contextId": "legacy-project",
                "itemType": "NOTE_DOCUMENT",
                "entityId": "doc-legacy",
                "order": 20,
                "updatedAt": 2,
                "isDeleted": false
              }
            ],
            "backlogOrders": [
              {
                "id": "legacy-order-1",
                "listId": "legacy-project",
                "itemId": "legacy-item-1",
                "order": 1,
                "updatedAt": 2,
                "isDeleted": false
              },
              {
                "id": "legacy-order-2",
                "listId": "legacy-project",
                "itemId": "legacy-item-2",
                "order": 2,
                "updatedAt": 2,
                "isDeleted": false
              }
            ]
          }
        }
        """
}
