const ringGroup = document.getElementById("ringGroup");
const ringTime = document.getElementById("ringTime");
const ringLabel = document.getElementById("ringLabel");
const challengeSection = document.getElementById("challengeSection");
const challengeQuestion = document.getElementById("challengeQuestion");
const challengeAnswer = document.getElementById("challengeAnswer");
const challengeFeedback = document.getElementById("challengeFeedback");
const submitChallengeBtn = document.getElementById("submitChallengeBtn");
const snoozeBtn = document.getElementById("snoozeBtn");
const dismissBtn = document.getElementById("dismissBtn");

let ringingState = null;

function pad2(n) {
    return String(n).padStart(2, "0");
}

window.onRingingState = function (json) {
    ringingState = JSON.parse(json);
    renderRingingState();
};

function renderRingingState() {
    if (!ringingState) return;

    const groupName = (ringingState.groupName || "").trim();
    const alarmLabel = (ringingState.label || "").trim();
    const hasCustomAlarmName = alarmLabel.length > 0 && alarmLabel !== groupName;

    ringTime.textContent = `${pad2(ringingState.hour)}:${pad2(ringingState.minute)}`;

    if (hasCustomAlarmName) {
        ringGroup.textContent = groupName;
        ringGroup.classList.remove("hidden");
        ringLabel.textContent = alarmLabel;
    } else {
        ringGroup.classList.add("hidden");
        ringLabel.textContent = groupName || alarmLabel || "Alarm";
    }
    snoozeBtn.textContent = `Snooze (${ringingState.snoozeMinutes}m)`;

    if (ringingState.challengeType === "math") {
        challengeSection.classList.remove("hidden");
        challengeQuestion.textContent = `${ringingState.challengeQuestion} = ?`;
    } else {
        challengeSection.classList.add("hidden");
    }

    dismissBtn.disabled = !ringingState.canDismiss;
}

submitChallengeBtn.addEventListener("click", () => {
    const result = JSON.parse(AndroidBridge.submitChallengeAnswer(challengeAnswer.value));
    challengeFeedback.className = "feedback " + (result.correct ? "ok" : "error");
    challengeFeedback.textContent = result.correct
        ? "Correct! You can dismiss the alarm."
        : "Wrong answer — try again.";
    if (result.canDismiss) {
        dismissBtn.disabled = false;
        challengeAnswer.disabled = true;
        submitChallengeBtn.disabled = true;
    }
});

snoozeBtn.addEventListener("click", () => AndroidBridge.snooze());

dismissBtn.addEventListener("click", () => {
    if (!dismissBtn.disabled) {
        AndroidBridge.dismiss();
    }
});

challengeAnswer.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
        submitChallengeBtn.click();
    }
});

document.addEventListener("DOMContentLoaded", () => {
    if (typeof AndroidBridge !== "undefined") {
        window.onRingingState(AndroidBridge.getRingingState());
    }
});
