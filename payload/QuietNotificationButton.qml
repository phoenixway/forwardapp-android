import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import qs.modules.components
import qs.modules.notifications
import qs.modules.services
import qs.modules.theme

// Quiet notification indicator for the RIGHT group of a horizontal Ambxst bar.
// Reuses Ambxst's BarPopup, NotificationDelegate and Notifications service.
Item {
    id: root
    property var bar

    readonly property bool pending: Notifications.quietUnreadCount > 0
    readonly property bool critical: Notifications.quietHasCriticalUnread

    // Remain mounted until the popup's close animation finishes: its window
    // anchor must not disappear immediately when messages become read.
    implicitWidth: (pending || historyPopup.visible) ? 28 : 0
    implicitHeight: 36
    Layout.preferredWidth: implicitWidth
    Layout.preferredHeight: implicitHeight
    visible: implicitWidth > 0

    Rectangle {
        anchors.centerIn: parent
        width: 25
        height: 26
        radius: 7
        color: root.critical ? Colors.errorContainer : "transparent"
        visible: root.pending
    }
    Text {
        anchors.centerIn: parent
        text: root.critical ? Icons.bellRinging : Icons.bell
        font.family: Icons.font
        font.pixelSize: 17
        color: root.critical ? Colors.overErrorContainer : (button.containsMouse ? Colors.overBackground : Colors.outline)
        visible: root.pending
    }
    MouseArea {
        id: button
        anchors.fill: parent
        enabled: root.pending || historyPopup.visible
        hoverEnabled: true
        cursorShape: Qt.PointingHandCursor
        onClicked: {
            if (!historyPopup.isOpen) {
                // Capture a snapshot. Messages arriving while the list is open
                // must NOT silently become read on close.
                historyPopup.seenIds = Notifications.quietUnreadList.map(n => n.id);
            }
            historyPopup.toggle();
        }
    }

    BarPopup {
        // Align right edge near the button to avoid going off-screen.
        anchor.rect.x: root.width - totalWidth
        id: historyPopup
        anchorItem: root
        bar: root.bar
        contentWidth: 390
        contentHeight: Math.min(460, Math.max(84, history.contentHeight + 48))
        popupPadding: 12
        groupId: "bar"
        property var seenIds: []

        onIsOpenChanged: {
            if (!isOpen && seenIds.length > 0) {
                Notifications.quietMarkRead(seenIds);
                seenIds = [];
            }
        }

        Column {
            anchors.fill: parent
            spacing: 7
            Text {
                width: parent.width
                height: 24
                text: "Notifications"
                color: Colors.overBackground
                font.bold: true
                font.pixelSize: 14
            }
            ListView {
                id: history
                width: parent.width
                height: parent.height - 31
                clip: true
                spacing: 7
                model: Notifications.list.slice().reverse()
                delegate: NotificationDelegate {
                    required property var modelData
                    width: history.width
                    height: implicitHeight
                    notificationObject: modelData
                    expanded: true
                    onlyNotification: true
                }
                ScrollBar.vertical: ScrollBar { policy: ScrollBar.AsNeeded }
            }
        }
    }
}
