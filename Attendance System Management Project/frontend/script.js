const API_BASE = "http://localhost:8080/api";

let currentRole = null;
let currentStudentRoll = null;

// DOM
const loginScreen         = document.getElementById("login-screen");
const appShell            = document.getElementById("app-shell");
const userRoleLabel       = document.getElementById("user-role-label");
const logoutBtn           = document.getElementById("logout-btn");

const roleSelect          = document.getElementById("role-select");
const roleLecturerBtn     = document.getElementById("role-lecturer-btn");
const roleStudentBtn      = document.getElementById("role-student-btn");

const lecturerLoginPanel  = document.getElementById("lecturer-login-panel");
const studentLoginPanel   = document.getElementById("student-login-panel");
const lecturerLoginForm   = document.getElementById("lecturer-login-form");
const studentLoginForm    = document.getElementById("student-login-form");
const backFromLecturerBtn = document.getElementById("back-from-lecturer");
const backFromStudentBtn  = document.getElementById("back-from-student");

const lecturerPortalSection = document.getElementById("lecturer-portal");
const studentPortalSection  = document.getElementById("student-portal");

const addStudentForm      = document.getElementById("add-student-form");
const attendanceForm      = document.getElementById("attendance-form");
const studentTableBody    = document.getElementById("studentTableBody");
const searchStudentInput  = document.getElementById("searchStudent");

const studentViewForm     = document.getElementById("student-view-form");
const studentResultDiv    = document.getElementById("studentResult");

function calculatePercentage(total, attended) {
    if (total <= 0) return 0;
    if (attended < 0) attended = 0;
    if (attended > total) attended = total;
    return (attended / total) * 100;
}

// Role selection
roleLecturerBtn?.addEventListener("click", () => {
    roleSelect.classList.add("hidden");
    lecturerLoginPanel.classList.remove("hidden");
    studentLoginPanel.classList.add("hidden");
});

roleStudentBtn?.addEventListener("click", () => {
    roleSelect.classList.add("hidden");
    studentLoginPanel.classList.remove("hidden");
    lecturerLoginPanel.classList.add("hidden");
});

backFromLecturerBtn?.addEventListener("click", () => {
    lecturerLoginPanel.classList.add("hidden");
    roleSelect.classList.remove("hidden");
    lecturerLoginForm.reset();
});

backFromStudentBtn?.addEventListener("click", () => {
    studentLoginPanel.classList.add("hidden");
    roleSelect.classList.remove("hidden");
    studentLoginForm.reset();
});

// Backend helpers
async function loginLecturer(username, password) {
    const url = `${API_BASE}/login/lecturer?username=${encodeURIComponent(username)}&password=${encodeURIComponent(password)}`;
    const res = await fetch(url, { method: "POST" });
    if (!res.ok) return { success: false, message: "Request failed" };
    return await res.json();
}

async function fetchStudentsFromServer() {
    const res = await fetch(`${API_BASE}/students`);
    if (!res.ok) return [];
    return await res.json();
}

async function addStudentToServer(name, rollNo) {
    const url = `${API_BASE}/students?name=${encodeURIComponent(name)}&rollNo=${encodeURIComponent(rollNo)}`;
    const res = await fetch(url, { method: "POST" });
    if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Failed to add student");
    }
}

async function deleteStudentFromServer(rollNo) {
    const url = `${API_BASE}/students?rollNo=${encodeURIComponent(rollNo)}`;
    const res = await fetch(url, { method: "DELETE" });
    if (!res.ok) {
        const text = await res.text();
        throw new Error(text || "Failed to delete student");
    }
}

async function updateAttendanceOnServer(rollNo, totalClasses, attendedClasses) {
    const url = `${API_BASE}/students/attendance?rollNo=${encodeURIComponent(rollNo)}&totalClasses=${encodeURIComponent(totalClasses)}&attendedClasses=${encodeURIComponent(attendedClasses)}`;
    const res = await fetch(url, { method: "POST" });
    if (!res.ok) {
        const text = await res.text();
        try {
            const data = JSON.parse(text);
            throw new Error(data.message || data.error || "Failed to update attendance");
        } catch {
            throw new Error(text || "Failed to update attendance");
        }
    }
}

async function fetchStudentByRoll(rollNo) {
    const url = `${API_BASE}/students/by-roll?rollNo=${encodeURIComponent(rollNo)}`;
    const res = await fetch(url);
    if (!res.ok) return null;
    return await res.json();
}

// Login handlers
lecturerLoginForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const username = document.getElementById("lecturerUsername").value.trim();
    const password = document.getElementById("lecturerPassword").value.trim();

    try {
        const result = await loginLecturer(username, password);
        if (result.success) {
            currentRole = "lecturer";
            currentStudentRoll = null;
            enterAppAsLecturer();
            await renderStudentTable();
        } else {
            alert(result.message || "Invalid credentials.");
        }
    } catch (err) {
        console.error(err);
        alert("Failed to contact server for login.");
    }
});

studentLoginForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const rollInput = document.getElementById("studentLoginRoll");
    const rollNo = parseInt(rollInput.value, 10);

    if (isNaN(rollNo)) {
        alert("Please enter a valid roll number.");
        return;
    }

    try {
        const student = await fetchStudentByRoll(rollNo);
        if (!student) {
            alert("No student found with this roll number. Ask lecturer to add you.");
            return;
        }
        currentRole = "student";
        currentStudentRoll = rollNo;
        enterAppAsStudent(student);
    } catch (err) {
        console.error(err);
        alert("Failed to contact server.");
    }
});

// Portal switching
function enterAppAsLecturer() {
    userRoleLabel.textContent = "Logged in as Lecturer";
    loginScreen.classList.add("hidden");
    appShell.classList.remove("hidden");
    lecturerPortalSection.classList.remove("hidden");
    studentPortalSection.classList.add("hidden");
}

function enterAppAsStudent(student) {
    userRoleLabel.textContent = `Logged in as Student (${student.name}, Roll: ${student.rollNo})`;
    loginScreen.classList.add("hidden");
    appShell.classList.remove("hidden");
    lecturerPortalSection.classList.add("hidden");
    studentPortalSection.classList.remove("hidden");

    const viewRollNoInput = document.getElementById("viewRollNo");
    if (viewRollNoInput) viewRollNoInput.value = student.rollNo;
    showStudentAttendance(student);
}

logoutBtn?.addEventListener("click", () => {
    currentRole = null;
    currentStudentRoll = null;

    appShell.classList.add("hidden");
    loginScreen.classList.remove("hidden");

    lecturerPortalSection.classList.add("hidden");
    studentPortalSection.classList.add("hidden");

    lecturerLoginPanel.classList.add("hidden");
    studentLoginPanel.classList.add("hidden");
    roleSelect.classList.remove("hidden");

    lecturerLoginForm?.reset();
    studentLoginForm?.reset();
});

// Lecturer portal: add/view/delete/attendance
addStudentForm?.addEventListener("submit", async (e) => {
    e.preventDefault();

    const nameInput = document.getElementById("studentName");
    const rollNoInput = document.getElementById("rollNo");

    const name = nameInput.value.trim();
    const rollNo = parseInt(rollNoInput.value, 10);

    if (!name || isNaN(rollNo)) {
        alert("Please enter valid name and roll number.");
        return;
    }

    try {
        await addStudentToServer(name, rollNo);
        alert("Student added successfully.");
        nameInput.value = "";
        rollNoInput.value = "";
        await renderStudentTable();
    } catch (err) {
        console.error(err);
        alert(err.message || "Error adding student.");
    }
});

async function renderStudentTable() {
    if (!studentTableBody) {
        console.warn("studentTableBody not found");
        return;
    }

    const searchTerm = (searchStudentInput?.value || "").trim().toLowerCase();
    studentTableBody.innerHTML = "";

    let students = [];
    try {
        students = await fetchStudentsFromServer();
        students.sort((a, b) => a.rollNo - b.rollNo);
        console.log("Loaded students from server:", students);
    } catch (err) {
        console.error("Error loading students:", err);
        alert("Failed to load students from server.");
        return;
    }

    students
        .filter((s) => {
            if (!searchTerm) return true;
            return (
                s.name.toLowerCase().includes(searchTerm) ||
                String(s.rollNo).includes(searchTerm)
            );
        })
        .forEach((s) => {
            const tr = document.createElement("tr");
            const percentage =
                s.percentage ?? calculatePercentage(s.totalClasses, s.attendedClasses);
            const eligible = percentage >= 75;

            tr.innerHTML = `
                <td>${s.rollNo}</td>
                <td>${s.name}</td>
                <td>${s.totalClasses}</td>
                <td>${s.attendedClasses}</td>
                <td>${percentage.toFixed(1)}%</td>
                <td>
                    <span class="status-pill ${eligible ? "status-ok" : "status-low"}">
                        ${eligible ? "Eligible" : "Low Attendance"}
                    </span>
                </td>
                <td class="actions-cell">
                    <button type="button" class="present-btn" onclick="handleMarkPresent(${s.rollNo})">
                        Present
                    </button>
                    <button type="button" class="absent-btn" onclick="handleMarkAbsent(${s.rollNo})">
                        Absent
                    </button>
                    <button type="button" class="delete-btn" onclick="handleDeleteStudent(${s.rollNo})">
                        Delete
                    </button>
                </td>
            `;
            studentTableBody.appendChild(tr);
        });
}

// ---------- row actions (delete / present / absent) ----------
window.handleDeleteStudent = async function (rollNo) {
    if (!confirm("Are you sure you want to delete this student?")) return;
    try {
        await deleteStudentFromServer(rollNo);
        await renderStudentTable();
    } catch (err) {
        console.error(err);
        alert(err.message || "Failed to delete student.");
    }
};

async function markOneClass(rollNo, isPresent) {
    try {
        const student = await fetchStudentByRoll(rollNo);
        if (!student) {
            alert("Student not found. Please add the student first.");
            return;
        }

        const newTotal = student.totalClasses + 1;
        const newAttended = student.attendedClasses + (isPresent ? 1 : 0);

        await updateAttendanceOnServer(rollNo, newTotal, newAttended);
        await renderStudentTable();
    } catch (err) {
        console.error(err);
        alert(err.message || "Failed to update attendance.");
    }
}

// called from buttons in the table
window.handleMarkPresent = function (rollNo) {
    markOneClass(rollNo, true);
};

window.handleMarkAbsent = function (rollNo) {
    markOneClass(rollNo, false);
};

// ---------- search box ----------
searchStudentInput?.addEventListener("input", () => {
    renderStudentTable();
});

// ---------- manual attendance form ----------
attendanceForm?.addEventListener("submit", async (e) => {
    e.preventDefault();

    const rollInput = document.getElementById("attendanceRollNo");
    const totalInput = document.getElementById("totalClasses");
    const attendedInput = document.getElementById("attendedClasses");

    const rollNo = parseInt(rollInput.value, 10);
    const total = parseInt(totalInput.value, 10);
    const attended = parseInt(attendedInput.value, 10);

    if (isNaN(rollNo) || isNaN(total) || isNaN(attended)) {
        alert("Please enter valid numeric values.");
        return;
    }
    if (total < 0 || attended < 0 || attended > total) {
        alert("Please enter valid attendance values.");
        return;
    }

    try {
        await updateAttendanceOnServer(rollNo, total, attended);
        alert("Attendance updated successfully.");
        totalInput.value = "";
        attendedInput.value = "";
        await renderStudentTable();
    } catch (err) {
        console.error(err);
        alert(err.message || "Failed to update attendance.");
    }
});

// ---------- Student portal ----------
studentViewForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const rollInput = document.getElementById("viewRollNo");
    const rollNo = parseInt(rollInput.value, 10);

    if (isNaN(rollNo)) {
        alert("Please enter a valid roll number.");
        return;
    }

    try {
        const student = await fetchStudentByRoll(rollNo);
        if (!student) {
            studentResultDiv.innerHTML = "<p>No student found with that roll number.</p>";
            studentResultDiv.classList.remove("hidden");
            return;
        }
        showStudentAttendance(student);
    } catch (err) {
        console.error(err);
        alert("Failed to load student from server.");
    }
});

function showStudentAttendance(student) {
    const percentage =
        student.percentage ?? calculatePercentage(student.totalClasses, student.attendedClasses);
    const eligible = percentage >= 75;

    studentResultDiv.innerHTML = `
        <div class="result-row">
            <span class="result-label">Name</span>
            <span class="result-value">${student.name}</span>
        </div>
        <div class="result-row">
            <span class="result-label">Roll Number</span>
            <span class="result-value">${student.rollNo}</span>
        </div>
        <div class="result-row">
            <span class="result-label">Total Classes</span>
            <span class="result-value">${student.totalClasses}</span>
        </div>
        <div class="result-row">
            <span class="result-label">Attended Classes</span>
            <span class="result-value">${student.attendedClasses}</span>
        </div>
        <div class="result-row">
            <span class="result-label">Attendance %</span>
            <span class="result-value">${percentage.toFixed(2)}%</span>
        </div>
        <div class="result-row">
            <span class="result-label">Status</span>
            <span class="result-value">
                <span class="badge ${eligible ? "badge-ok" : "badge-bad"}">
                    ${eligible ? "Eligible for Exam" : "Not Eligible"}
                </span>
            </span>
        </div>
    `;
    studentResultDiv.classList.remove("hidden");
}
