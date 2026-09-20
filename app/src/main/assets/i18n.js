let i18n = {
    difficulty_labels: ["", "Easy", "Light", "Normal", "Hard", "Expert"],
    day_labels: ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"],
    first_day_of_week: 1,
};

/** Calendar.SUNDAY = 1 … Calendar.SATURDAY = 7; matches alarm repeat day bits. */
function firstDayOfWeek() {
    const value = Number(i18n.first_day_of_week);
    return value >= 1 && value <= 7 ? value : 1;
}

function calendarDayToBit(calendarDay) {
    return 1 << (calendarDay - 1);
}

function dayOrderCalendarDays() {
    const first = firstDayOfWeek();
    const order = [];
    for (let i = 0; i < 7; i++) {
        order.push(((first - 1 + i) % 7) + 1);
    }
    return order;
}

function labelForCalendarDay(calendarDay) {
    const labels = i18n.day_labels || [];
    return labels[calendarDay - 1] || "";
}

function layoutRepeatDayGrid() {
    const container = document.getElementById("repeatDays");
    if (!container) return;

    const byBit = new Map();
    container.querySelectorAll("label").forEach((label) => {
        const input = label.querySelector("input[data-day]");
        if (!input) return;
        byBit.set(Number(input.dataset.day), label);
    });

    container.replaceChildren();
    dayOrderCalendarDays().forEach((calendarDay) => {
        const bit = calendarDayToBit(calendarDay);
        const label = byBit.get(bit);
        if (!label) return;
        const span = label.querySelector("span");
        if (span) span.textContent = labelForCalendarDay(calendarDay);
        container.appendChild(label);
    });
}

function t(key) {
    const value = i18n[key];
    return value === undefined || value === null ? key : value;
}

function tf(key, ...args) {
    let s = t(key);
    args.forEach((val, i) => {
        const n = i + 1;
        s = s.replace(new RegExp(`%${n}\\$s`, "g"), val);
        s = s.replace(new RegExp(`%${n}\\$d`, "g"), String(val));
    });
    return s;
}

function loadI18nFromBridge() {
    if (typeof AndroidBridge === "undefined" || typeof AndroidBridge.getUiStrings !== "function") {
        return;
    }
    try {
        i18n = JSON.parse(AndroidBridge.getUiStrings());
    } catch {
        /* keep fallbacks */
    }
}

function applyMainPageI18n() {
    const textById = {
        addGroupBtn: "ui_new_group",
        groupDialogTitle: "ui_group",
        deleteGroupBtn: "ui_delete_group",
        cancelGroupBtn: "ui_cancel",
        saveGroupBtn: "ui_save",
        alarmDialogTitle: "ui_alarm",
        deleteAlarmBtn: "ui_delete_alarm",
        cancelAlarmBtn: "ui_cancel",
        saveAlarmBtn: "ui_save",
        permissionTitle: "ui_permission",
        permissionNo: "ui_no_thanks",
        permissionYes: "ui_continue",
        closeSettingsBtn: "ui_done",
        confirmTitle: "ui_confirm",
        confirmNo: "ui_cancel",
        confirmYes: "ui_delete",
        submitChallengeBtn: "ui_ringing_submit",
        dismissBtn: "ui_ringing_dismiss",
    };
    Object.entries(textById).forEach(([id, key]) => {
        const el = document.getElementById(id);
        if (el) el.textContent = t(key);
    });

    const labelText = {
        labelGroupName: "ui_name",
        labelGroupEnabled: "ui_group_enabled",
        labelGroupOverrideDnd: "ui_override_dnd",
        labelGroupChallenge: "ui_wake_challenge",
        labelGroupDifficulty: "ui_math_difficulty",
        labelGroupSnooze: "ui_snooze_minutes",
        labelGroupVolumeRamp: "ui_volume_ramp_seconds",
        labelAlarmTime: "ui_time",
        labelAlarmLabel: "ui_label",
        labelAlarmEnabled: "ui_alarm_enabled",
        labelSettingShowNextAlarm: "ui_show_next_alarm_notification",
    };
    Object.entries(labelText).forEach(([id, key]) => {
        const el = document.getElementById(id);
        if (el) el.textContent = t(key);
    });

    const settingsTitle = document.querySelector("#settingsDialog h3");
    if (settingsTitle) settingsTitle.textContent = t("ui_settings");

    const repeatLegend = document.querySelector("#alarmForm legend");
    if (repeatLegend) repeatLegend.textContent = t("ui_repeat");

    const hints = {
        groupVolumeRampHint: "ui_volume_ramp_hint",
        repeatHint: "ui_repeat_hint",
        settingShowNextAlarmHint: "ui_show_next_alarm_hint",
    };
    Object.entries(hints).forEach(([id, key]) => {
        const el = document.getElementById(id);
        if (el) el.textContent = t(key);
    });

    const permissionNote = document.getElementById("permissionNote");
    if (permissionNote) permissionNote.innerHTML = t("ui_permission_note");

    const settingsBtn = document.getElementById("settingsBtn");
    if (settingsBtn) settingsBtn.setAttribute("aria-label", t("ui_settings"));

    const alarmLabelInput = document.getElementById("alarmLabel");
    if (alarmLabelInput) alarmLabelInput.placeholder = t("ui_label_placeholder");

    const challengeTitle = document.getElementById("challengeTitle");
    if (challengeTitle) challengeTitle.textContent = t("ui_ringing_solve");

    const challengeAnswer = document.getElementById("challengeAnswer");
    if (challengeAnswer) challengeAnswer.placeholder = t("ui_ringing_answer_placeholder");

    const snoozeBtn = document.getElementById("snoozeBtn");
    if (snoozeBtn) snoozeBtn.textContent = t("ui_ringing_snooze");

    const settingsPermissionsTitle = document.getElementById("settingsPermissionsTitle");
    if (settingsPermissionsTitle) {
        settingsPermissionsTitle.textContent = t("ui_settings_permissions");
    }

    layoutRepeatDayGrid();

    document.documentElement.lang = document.documentElement.lang || "en";
}

function applyRingingPageI18n() {
    applyMainPageI18n();
}
