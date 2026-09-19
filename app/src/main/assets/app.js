// UTF-8 safe Base64 decode (atob alone breaks accented characters on Android WebView)
window.decodePayload = function (encoded) {
    const binary = atob(encoded);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    if (typeof TextDecoder !== "undefined") {
        return JSON.parse(new TextDecoder("utf-8").decode(bytes));
    }
    let percentEncoded = "";
    for (let i = 0; i < bytes.length; i++) {
        percentEncoded += "%" + ("00" + bytes[i].toString(16)).slice(-2);
    }
    return JSON.parse(decodeURIComponent(percentEncoded));
};

let state = { groups: [], alarms: [] };
let permissions = null;
let appSettings = { showNextAlarmNotification: true };
let challengeTypes = [];
const expandedGroups = new Set();
let confirmCallback = null;

const DIFFICULTY_LABELS = ["", "Easy", "Light", "Normal", "Hard", "Expert"];

const alarmTree = document.getElementById("alarmTree");
const permissionBanner = document.getElementById("permissionBanner");
const groupDialog = document.getElementById("groupDialog");
const alarmDialog = document.getElementById("alarmDialog");
const permissionDialog = document.getElementById("permissionDialog");
const confirmDialog = document.getElementById("confirmDialog");
const settingsDialog = document.getElementById("settingsDialog");

const DAY_LABELS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

function pad2(n) {
    return String(n).padStart(2, "0");
}

function formatTime(hour, minute) {
    return `${pad2(hour)}:${pad2(minute)}`;
}

function repeatLabel(repeatDays) {
    if (!repeatDays) return "Daily";
    const days = DAY_LABELS.filter((_, i) => repeatDays & (1 << i));
    if (days.length === 7) return "Every day";
    return days.join(", ");
}

function escapeHtml(text) {
    return String(text)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

function normalizeState(data) {
    if (!data) return { groups: [], alarms: [] };
    if (typeof data === "string") {
        try {
            return JSON.parse(data);
        } catch {
            return { groups: [], alarms: [] };
        }
    }
    return {
        groups: Array.isArray(data.groups) ? data.groups : [],
        alarms: Array.isArray(data.alarms) ? data.alarms : [],
    };
}

function openOverlay(el) {
    el.classList.remove("hidden");
    el.setAttribute("aria-hidden", "false");
}

function closeOverlay(el) {
    el.classList.add("hidden");
    el.setAttribute("aria-hidden", "true");
}

function showConfirm(title, message, onConfirm) {
    document.getElementById("confirmTitle").textContent = title;
    document.getElementById("confirmMessage").textContent = message;
    confirmCallback = onConfirm;
    openOverlay(confirmDialog);
}

window.onStateChanged = function (data) {
    state = normalizeState(data);
    renderTree();
};

window.onSettingsChanged = function (data) {
    if (!data) {
        appSettings = { showNextAlarmNotification: true };
    } else if (typeof data === "string") {
        try {
            appSettings = JSON.parse(data);
        } catch {
            appSettings = { showNextAlarmNotification: true };
        }
    } else {
        appSettings = data;
    }
    const checkbox = document.getElementById("settingShowNextAlarm");
    if (checkbox && document.activeElement !== checkbox) {
        checkbox.checked = appSettings.showNextAlarmNotification !== false;
    }
};

window.onPermissionsChanged = function (data) {
    if (!data) {
        permissions = null;
    } else if (typeof data === "string") {
        try {
            permissions = JSON.parse(data);
        } catch {
            permissions = null;
        }
    } else {
        permissions = data;
    }
    renderPermissionBanner();
};

function loadChallengeTypes() {
    if (typeof AndroidBridge === "undefined") return;
    challengeTypes = JSON.parse(AndroidBridge.getChallengeTypes());
    document.getElementById("groupChallenge").innerHTML = challengeTypes
        .map((c) => `<option value="${c.id}">${c.label}</option>`)
        .join("");
}

function groupMeta(group) {
    const parts = [
        group.challengeType === "math"
            ? `Math - ${DIFFICULTY_LABELS[group.challengeDifficulty || 3]}`
            : "No challenge",
        `Snooze ${group.snoozeMinutes}m`,
    ];
    if (group.overrideDnd) parts.push("DND override");
    if (group.volumeRampSeconds > 0) parts.push(`Vol ramp ${group.volumeRampSeconds}s`);
    return parts.join(" - ");
}

function isGroupExpanded(groupId) {
    return expandedGroups.has(String(groupId));
}

function renderTree() {
    if (!state.groups.length) {
        alarmTree.innerHTML = '<p class="tree-empty">No groups yet. Tap "New group" below.</p>';
        return;
    }

    const alarmsByGroup = {};
    state.alarms.forEach((alarm) => {
        if (!alarmsByGroup[alarm.groupId]) alarmsByGroup[alarm.groupId] = [];
        alarmsByGroup[alarm.groupId].push(alarm);
    });

    alarmTree.innerHTML = state.groups
        .map((group) => {
            const expanded = isGroupExpanded(group.id);
            const alarms = (alarmsByGroup[group.id] || []).sort(
                (a, b) => a.hour * 60 + a.minute - (b.hour * 60 + b.minute),
            );

            const alarmRows = alarms.length
                ? alarms
                      .map((alarm) => {
                          const active = alarm.enabled && group.enabled;
                          return `
                            <div class="tree-alarm ${active ? "" : "inactive"}" data-alarm-id="${alarm.id}">
                                <div class="tree-alarm-main">
                                    <div class="tree-alarm-time">${formatTime(alarm.hour, alarm.minute)}</div>
                                    <div class="tree-alarm-info">
                                        <div class="tree-alarm-label">${escapeHtml(alarm.label || "Alarm")}</div>
                                        <div class="tree-alarm-meta">${repeatLabel(alarm.repeatDays)}</div>
                                    </div>
                                </div>
                                <button type="button" class="toggle toggle-sm ${alarm.enabled ? "on" : ""}" data-action="toggle-alarm" aria-label="Toggle alarm"></button>
                            </div>`;
                      })
                      .join("")
                : '<p class="tree-empty">No alarms in this group.</p>';

            return `
                <section class="tree-group" data-group-id="${group.id}">
                    <div class="tree-group-head">
                        <button type="button" class="tree-chevron" data-action="toggle-collapse" aria-label="Expand or collapse">${expanded ? "&#9660;" : "&#9654;"}</button>
                        <div class="tree-group-info">
                            <div class="tree-group-name">${escapeHtml(group.name)}</div>
                            <div class="tree-group-meta">${groupMeta(group)}</div>
                        </div>
                        <button type="button" class="toggle toggle-sm ${group.enabled ? "on" : ""}" data-action="toggle-group" aria-label="Toggle group"></button>
                    </div>
                    <div class="tree-children ${expanded ? "" : "collapsed"}">
                        ${alarmRows}
                        <button type="button" class="tree-add-alarm" data-action="add-alarm">+ Add alarm</button>
                    </div>
                </section>`;
        })
        .join("");
}

function renderPermissionBanner() {
    if (!permissions) {
        permissionBanner.classList.add("hidden");
        permissionBanner.innerHTML = "";
        return;
    }

    const items = [
        { key: "notifications", label: "Notifications", kind: "notifications" },
        { key: "exactAlarms", label: "Exact alarms", kind: "exactAlarms" },
        { key: "dnd", label: "Do Not Disturb access", kind: "dnd" },
    ];

    const missing = items.filter(({ key }) => {
        const entry = permissions[key];
        return entry && !entry.granted;
    });

    if (!missing.length) {
        permissionBanner.classList.add("hidden");
        permissionBanner.innerHTML = "";
        return;
    }

    permissionBanner.classList.remove("hidden");
    permissionBanner.innerHTML = missing
        .map(({ key, label, kind }) => {
            const entry = permissions[key];
            return `
                <div class="perm-card">
                    <p><strong>${label}</strong> - ${entry.feature}.</p>
                    <button type="button" class="btn-primary" data-perm-kind="${kind}">Allow ${label.toLowerCase()}</button>
                </div>`;
        })
        .join("");
}

let pendingPermissionKind = null;

function showPermissionDialog(kind) {
    if (!permissions || !permissions[kind]) return;

    const entry = permissions[kind];
    const titles = {
        notifications: "Allow notifications?",
        exactAlarms: "Allow exact alarms?",
        dnd: "Allow Do Not Disturb access?",
    };
    const bodies = {
        notifications:
            "Notifications show your next alarm in the shade and the alarm on the lock screen when it rings.",
        exactAlarms:
            "Exact alarms let the app ring at the precise time you set, even when the phone is idle.",
        dnd:
            'DND access lets groups marked "Override Do Not Disturb" ring through silent mode.',
    };

    pendingPermissionKind = kind;
    document.getElementById("permissionTitle").textContent = titles[kind];
    document.getElementById("permissionBody").textContent =
        `${bodies[kind]} Without it, ${entry.feature.toLowerCase()} may not work reliably.`;
    openOverlay(permissionDialog);
}

function updateDifficultyUi() {
    const challenge = document.getElementById("groupChallenge").value;
    const block = document.getElementById("difficultyBlock");
    block.classList.toggle("hidden", challenge !== "math");

    const value = Number(document.getElementById("groupDifficulty").value);
    document.getElementById("groupDifficultyLabel").textContent =
        DIFFICULTY_LABELS[value] || "Normal";
}

function openGroupDialog(group) {
    document.getElementById("groupDialogTitle").textContent = group ? "Edit group" : "New group";
    document.getElementById("deleteGroupBtn").classList.toggle("hidden", !group);
    document.getElementById("groupId").value = group?.id || 0;
    document.getElementById("groupName").value = group?.name || "";
    document.getElementById("groupEnabled").checked = group?.enabled ?? true;
    document.getElementById("groupOverrideDnd").checked = group?.overrideDnd ?? false;
    document.getElementById("groupChallenge").value = group?.challengeType || "none";
    document.getElementById("groupDifficulty").value = group?.challengeDifficulty || 3;
    document.getElementById("groupSnooze").value = group?.snoozeMinutes ?? 9;
    document.getElementById("groupVolumeRamp").value = group?.volumeRampSeconds ?? 0;
    updateDifficultyUi();
    openOverlay(groupDialog);
    document.getElementById("groupName").focus();
}

function openAlarmDialog(alarm, groupId) {
    document.getElementById("alarmDialogTitle").textContent = alarm ? "Edit alarm" : "New alarm";
    document.getElementById("deleteAlarmBtn").classList.toggle("hidden", !alarm);
    document.getElementById("alarmId").value = alarm?.id || 0;
    document.getElementById("alarmGroupId").value = groupId || alarm?.groupId || state.groups[0]?.id || 0;
    document.getElementById("alarmLabel").value = alarm?.label || "";
    document.getElementById("alarmTime").value = alarm
        ? `${pad2(alarm.hour)}:${pad2(alarm.minute)}`
        : "07:00";
    document.getElementById("alarmEnabled").checked = alarm?.enabled ?? true;

    document.querySelectorAll("#repeatDays input[data-day]").forEach((input) => {
        const bit = Number(input.dataset.day);
        input.checked = alarm ? (alarm.repeatDays & bit) !== 0 : false;
    });

    openOverlay(alarmDialog);
    requestAnimationFrame(() => {
        document.getElementById("alarmTime").focus();
    });
}

function readGroupForm() {
    return {
        id: Number(document.getElementById("groupId").value),
        name: document.getElementById("groupName").value.trim(),
        enabled: document.getElementById("groupEnabled").checked,
        overrideDnd: document.getElementById("groupOverrideDnd").checked,
        challengeType: document.getElementById("groupChallenge").value,
        challengeDifficulty: Number(document.getElementById("groupDifficulty").value),
        snoozeMinutes: Number(document.getElementById("groupSnooze").value),
        volumeRampSeconds: Math.max(
            0,
            Number(document.getElementById("groupVolumeRamp").value) || 0,
        ),
    };
}

function readAlarmForm() {
    const time = document.getElementById("alarmTime").value.split(":");
    let repeatDays = 0;
    document.querySelectorAll("#repeatDays input[data-day]:checked").forEach((input) => {
        repeatDays |= Number(input.dataset.day);
    });
    return {
        id: Number(document.getElementById("alarmId").value),
        groupId: Number(document.getElementById("alarmGroupId").value),
        hour: Number(time[0]),
        minute: Number(time[1]),
        label: document.getElementById("alarmLabel").value.trim(),
        enabled: document.getElementById("alarmEnabled").checked,
        repeatDays,
    };
}

function saveGroup() {
    const group = readGroupForm();
    if (!group.name) {
        document.getElementById("groupName").focus();
        return;
    }
    closeOverlay(groupDialog);
    AndroidBridge.saveGroup(JSON.stringify(group));
}

function saveAlarm() {
    const alarm = readAlarmForm();
    if (!document.getElementById("alarmTime").value) {
        document.getElementById("alarmTime").focus();
        return;
    }
    closeOverlay(alarmDialog);
    AndroidBridge.saveAlarm(JSON.stringify(alarm));
}

function deleteGroupConfirmed() {
    const groupId = document.getElementById("groupId").value;
    expandedGroups.delete(groupId);
    closeOverlay(groupDialog);
    AndroidBridge.deleteGroup(groupId);
}

function deleteAlarmConfirmed() {
    const alarmId = document.getElementById("alarmId").value;
    closeOverlay(alarmDialog);
    AndroidBridge.deleteAlarm(alarmId);
}

document.getElementById("addGroupBtn").addEventListener("click", () => openGroupDialog(null));
document.getElementById("cancelGroupBtn").addEventListener("click", () => closeOverlay(groupDialog));
document.getElementById("cancelAlarmBtn").addEventListener("click", () => closeOverlay(alarmDialog));
document.getElementById("saveGroupBtn").addEventListener("click", saveGroup);
document.getElementById("saveAlarmBtn").addEventListener("click", saveAlarm);

document.getElementById("deleteGroupBtn").addEventListener("click", () => {
    const group = readGroupForm();
    if (!group.id) return;
    showConfirm(
        "Delete group?",
        `Delete "${group.name}" and all alarms inside it? This cannot be undone.`,
        deleteGroupConfirmed,
    );
});

document.getElementById("deleteAlarmBtn").addEventListener("click", () => {
    const alarm = readAlarmForm();
    if (!alarm.id) return;
    const label = alarm.label || "Alarm";
    showConfirm(
        "Delete alarm?",
        `Delete "${label}"? This cannot be undone.`,
        deleteAlarmConfirmed,
    );
});

document.getElementById("confirmNo").addEventListener("click", () => {
    confirmCallback = null;
    closeOverlay(confirmDialog);
});

document.getElementById("confirmYes").addEventListener("click", () => {
    const callback = confirmCallback;
    confirmCallback = null;
    closeOverlay(confirmDialog);
    if (callback) callback();
});

document.getElementById("groupOverrideDnd").addEventListener("change", (e) => {
    if (!e.target.checked || !permissions || permissions.dnd?.granted) return;
    showPermissionDialog("dnd");
});

document.getElementById("groupChallenge").addEventListener("change", updateDifficultyUi);
document.getElementById("groupDifficulty").addEventListener("input", updateDifficultyUi);

document.getElementById("groupForm").addEventListener("submit", (e) => e.preventDefault());
document.getElementById("alarmForm").addEventListener("submit", (e) => e.preventDefault());

function openSettingsDialog() {
    document.getElementById("settingShowNextAlarm").checked =
        appSettings.showNextAlarmNotification !== false;
    openOverlay(settingsDialog);
}

document.getElementById("settingsBtn").addEventListener("click", openSettingsDialog);
document.getElementById("closeSettingsBtn").addEventListener("click", () => closeOverlay(settingsDialog));

document.getElementById("settingShowNextAlarm").addEventListener("change", (e) => {
    if (typeof AndroidBridge === "undefined") return;
    const enabled = e.target.checked;
    appSettings.showNextAlarmNotification = enabled;
    AndroidBridge.setShowNextAlarmNotification(enabled);
    if (enabled && permissions && !permissions.notifications?.granted) {
        showPermissionDialog("notifications");
    }
});

[groupDialog, alarmDialog, permissionDialog, confirmDialog, settingsDialog].forEach((overlay) => {
    overlay.addEventListener("click", (e) => {
        if (e.target === overlay) closeOverlay(overlay);
    });
    overlay.querySelector(".modal")?.addEventListener("click", (e) => e.stopPropagation());
});

alarmTree.addEventListener("click", (e) => {
    const actionEl = e.target.closest("[data-action]");
    const action = actionEl?.dataset.action;

    if (action === "toggle-collapse") {
        const groupSection = actionEl.closest("[data-group-id]");
        const groupId = groupSection.dataset.groupId;
        if (expandedGroups.has(groupId)) expandedGroups.delete(groupId);
        else expandedGroups.add(groupId);
        renderTree();
        return;
    }

    if (action === "toggle-group") {
        const groupSection = actionEl.closest("[data-group-id]");
        const groupId = groupSection.dataset.groupId;
        const group = state.groups.find((g) => String(g.id) === groupId);
        AndroidBridge.setGroupEnabled(groupId, !group.enabled);
        return;
    }

    if (action === "toggle-alarm") {
        const alarmRow = actionEl.closest("[data-alarm-id]");
        const alarmId = alarmRow.dataset.alarmId;
        const alarm = state.alarms.find((a) => String(a.id) === alarmId);
        AndroidBridge.setAlarmEnabled(alarmId, !alarm.enabled);
        return;
    }

    if (action === "add-alarm") {
        const groupSection = actionEl.closest("[data-group-id]");
        openAlarmDialog(null, Number(groupSection.dataset.groupId));
        return;
    }

    const alarmMain = e.target.closest(".tree-alarm-main");
    if (alarmMain) {
        const alarmRow = alarmMain.closest("[data-alarm-id]");
        const alarm = state.alarms.find((a) => String(a.id) === alarmRow.dataset.alarmId);
        openAlarmDialog(alarm);
        return;
    }

    const groupInfo = e.target.closest(".tree-group-info");
    if (groupInfo) {
        const groupSection = groupInfo.closest("[data-group-id]");
        const group = state.groups.find((g) => String(g.id) === groupSection.dataset.groupId);
        openGroupDialog(group);
    }
});

permissionBanner.addEventListener("click", (e) => {
    const btn = e.target.closest("[data-perm-kind]");
    if (btn) showPermissionDialog(btn.dataset.permKind);
});

document.getElementById("permissionNo").addEventListener("click", () => {
    pendingPermissionKind = null;
    closeOverlay(permissionDialog);
});

document.getElementById("permissionYes").addEventListener("click", () => {
    if (pendingPermissionKind && typeof AndroidBridge !== "undefined") {
        AndroidBridge.requestPermission(pendingPermissionKind);
    }
    pendingPermissionKind = null;
    closeOverlay(permissionDialog);
});

document.addEventListener("DOMContentLoaded", () => {
    loadChallengeTypes();
    if (typeof AndroidBridge !== "undefined") {
        window.onStateChanged(AndroidBridge.getState());
        window.onPermissionsChanged(AndroidBridge.getPermissions());
        window.onSettingsChanged(AndroidBridge.getSettings());
    }
});
