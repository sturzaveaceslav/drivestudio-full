// admin.js complet funcțional pentru DriveStudio - FEX.NET style
let allGalleries = [];
let allUsers = [];
let currentFolderId = null;
let currentXHR = null;
let isUploading = false;


fetch("/api/admin/check-session", { credentials: "include" })
    .then(res => {
        if (!res.ok) throw new Error("Nu ești logat sau nu ai drepturi admin");
        return res.text();
    })
    .then(() => {
        loadGalleries();
        loadUsers();
        loadFoldersAndFiles();
    })
    .catch(err => {
        alert("❌ " + err.message);
        window.location.href = "/login.html";
    });

window.addEventListener("beforeunload", function (e) {
    if (isUploading) {
        e.preventDefault();
        e.returnValue = 'Fișierele sunt în curs de încărcare. Sigur vrei să închizi?';
    }
});


function loadGalleries() {
    fetch("/api/admin/galleries", { credentials: "include" })
        .then(res => res.json())
        .then(galleries => {
            allGalleries = galleries;
            renderGalleries();
        });
}

function renderGalleries() {
    const search = document.getElementById("gallerySearch").value.toLowerCase();
    const sort = document.getElementById("gallerySort").value;
    const filtered = allGalleries.filter(g => (g.folderName || "").toLowerCase().includes(search));
    filtered.sort((a, b) => {
        if (sort === "date-desc") return new Date(b.uploadDate) - new Date(a.uploadDate);
        if (sort === "date-asc") return new Date(a.uploadDate) - new Date(b.uploadDate);
        if (sort === "name-asc") return (a.folderName || "").localeCompare(b.folderName || "");
        if (sort === "name-desc") return (b.folderName || "").localeCompare(a.folderName || "");


    });

    document.getElementById("gallery-rows-admin").innerHTML = "";
    document.getElementById("gallery-rows-user").innerHTML = "";
    document.getElementById("gallery-rows-anonim").innerHTML = "";


    filtered.forEach(g => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${g.galleryId}</td>
            <td>${new Date(g.uploadDate).toLocaleString()}</td>
            <td>${g.uploader}</td>
            <td>${g.uploader === 'anonim' ? 'anonim' : (['slavon','vlad'].includes(g.uploader) ? 'admin' : 'user')}</td>
            <td>${g.fileCount}</td>
            <td>${g.folderName || '-'}</td>
            <td>
                <button class="btn btn-view" onclick="viewGallery('${g.galleryId}')">Deschide</button>
                <button class="btn btn-download" onclick="downloadZip('${g.galleryId}')">ZIP</button>
                <button class="btn btn-delete" onclick="deleteGallery('${g.galleryId}', this)">Șterge</button>
            </td>
        `;
        if (g.uploader === 'anonim') document.getElementById("gallery-rows-anonim").appendChild(tr);
        else if (['slavon','vlad'].includes(g.uploader)) document.getElementById("gallery-rows-admin").appendChild(tr);
        else document.getElementById("gallery-rows-user").appendChild(tr);
    });
    document.getElementById("count-admin").innerText = document.getElementById("gallery-rows-admin").children.length + " galerii";
    document.getElementById("count-user").innerText = document.getElementById("gallery-rows-user").children.length + " galerii";
    document.getElementById("count-anonim").innerText = document.getElementById("gallery-rows-anonim").children.length + " galerii";
}

function loadUsers() {
    fetch("/api/admin/users", { credentials: "include" })
        .then(res => res.json())
        .then(users => {
            allUsers = users;
            renderUsers();
        });
}

function renderUsers() {
    const search = document.getElementById("userSearch").value.toLowerCase();
    const tbody = document.getElementById("user-rows");
    tbody.innerHTML = "";
    allUsers.filter(u => u.username.toLowerCase().includes(search)).forEach(u => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${u.id}</td>
            <td>${u.username}</td>
            <td>${u.role}</td>
            <td><input type="number" value="${u.maxUploadSize}" min="0" style="width: 120px"> MB</td>
            <td>
                <button class="btn btn-delete" onclick="deleteUser(${u.id}, this)">Șterge</button>
                <button class="btn btn-download" onclick="updateLimit(${u.id}, this)">Actualizează</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

document.getElementById("upload-form").addEventListener("submit", function (e) {
    e.preventDefault();
    const files = document.getElementById("fileInput").files;
    const customName = document.getElementById("customName").value;
    const formData = new FormData();

    if (files.length === 0) return alert("Selectează fișiere!");
    formData.append("customName", customName);
    if (currentFolderId !== null) formData.append("folderId", currentFolderId);
    for (const file of files) formData.append("files", file);

    const xhr = new XMLHttpRequest();
    currentXHR = xhr;
    isUploading = true;

    let startTime = null;

    xhr.open("POST", "/upload2", true);
    xhr.withCredentials = true;

    xhr.upload.addEventListener("progress", function (e) {
        if (e.lengthComputable) {
            const now = new Date().getTime();
            if (!startTime) startTime = now;

            const percent = Math.round((e.loaded / e.total) * 100);
            document.getElementById("progress-fill").style.width = percent + "%";
            document.getElementById("progress-percent").innerText = percent + "%";

            const timeElapsedSec = (now - startTime) / 1000;
            const speedMBps = e.loaded / 1024 / 1024 / timeElapsedSec;
            const remainingBytes = e.total - e.loaded;
            const timeRemainingSec = remainingBytes / (speedMBps * 1024 * 1024);

            document.getElementById("uploadSpeed").innerText =
                `🚀 ${speedMBps.toFixed(2)} MB/s | ⏳ Estimare: ${Math.round(timeRemainingSec)} sec.`;
        }
    });

    xhr.onload = function () {
        isUploading = false;
        document.getElementById("uploadSpeed").innerText = "";
        if (xhr.status === 200) {
            alert("✅ Upload complet!");
            document.getElementById("progress-fill").style.width = "0%";
            document.getElementById("progress-percent").innerText = "0%";
            document.getElementById("fileInput").value = "";
            loadGalleries();
            loadFoldersAndFiles();
        } else {
            alert("❌ Eroare la încărcare: " + xhr.responseText);
        }
    };

    xhr.send(formData);
});

document.getElementById("cancelUploadBtn").addEventListener("click", () => {
    if (currentXHR) {
        currentXHR.abort();
        isUploading = false;
        document.getElementById("progress-fill").style.width = "0%";
        document.getElementById("progress-percent").innerText = "Anulat";
        alert("⚠️ Upload anulat.");
    }
});



function viewGallery(id) {
    window.open(`/preview.html?id=${id}`, "_blank");
}

function downloadZip(id) { window.open(`/api/admin/galleries/${id}/download-zip`, "_blank"); }
function deleteGallery(id, btn) {
    if (!confirm("Ești sigur că vrei să ștergi această galerie?")) return;
    fetch(`/api/admin/galleries/${id}`, { method: "DELETE", credentials: "include" })
        .then(res => { if (res.ok) btn.closest("tr").remove(); else alert("❌ Eroare la ștergere galerie"); });
}
function deleteUser(id, btn) {
    if (!confirm("Ștergi acest utilizator?")) return;
    fetch(`/api/admin/users/${id}`, { method: "DELETE", credentials: "include" })
        .then(res => { if (res.ok) btn.closest("tr").remove(); else alert("❌ Eroare la ștergere utilizator"); });
}
function updateLimit(id, btn) {
    const input = btn.parentElement.previousElementSibling.querySelector("input");
    const value = parseInt(input.value);
    if (isNaN(value) || value < 0) return alert("❌ Limită invalidă!");
    fetch(`/api/admin/users/${id}`, {
        method: "PUT",
        headers: { 'Content-Type': 'application/json' },
        credentials: "include",
        body: JSON.stringify({ maxUploadSize: value })
    })
        .then(res => res.text())
        .then(msg => alert("✅ " + msg))
        .catch(err => alert("❌ " + err.message));
}

// FOLDERE
// FOLDERE (corectat complet)
let breadcrumb = [{ id: null, name: "PAGINA PRINCIPALA" }];
function loadFoldersAndFiles() {
    let url = `/api/user/folders${currentFolderId ? '?parentId=' + currentFolderId : ''}`;
    fetch(url, { credentials: 'include' })
        .then(res => {
            if (!res.ok) throw new Error("Eroare la încărcare foldere");
            return res.json();
        })
        .then(data => {
            renderBreadcrumb();
            renderFolders(data.subfolders || data);
            loadFiles(currentFolderId);
        })
        .catch(err => alert("❌ " + err.message));
}

function loadFiles(folderId) {
    fetch(`/api/user/files${folderId ? '?folderId=' + folderId : ''}`, { credentials: 'include' })
        .then(res => res.json())
        .then(files => renderFiles(files));
}

function renderBreadcrumb() {
    const breadcrumbDiv = document.getElementById("breadcrumb");
    breadcrumbDiv.innerHTML = breadcrumb.map((b, i) => `
        <span class="breadcrumb-part" onclick="navigateToBreadcrumb(${i})">${b.name}</span>
    `).join(' / ');
}

function navigateToBreadcrumb(index) {
    breadcrumb = breadcrumb.slice(0, index + 1);
    currentFolderId = breadcrumb[index].id;
    document.getElementById("current-folder-name").innerHTML = `Încarci în: <strong>${breadcrumb[index].name}</strong>`;
    loadFoldersAndFiles();
}

function navigateToFolder(folderId, folderName) {
    currentFolderId = folderId;
    breadcrumb.push({ id: folderId, name: folderName });
    document.getElementById("current-folder-name").innerHTML = `Încarci în: <strong>${folderName}</strong>`;
    loadFoldersAndFiles();
}

function renderFolders(folders) {
    const container = document.getElementById("folder-container");
    container.innerHTML = "";
    folders.forEach(folder => {
        const div = document.createElement("div");
        div.className = "folder-card";
        div.innerHTML = `
    <img src="/img/folder.png" alt="Folder">
    <span class="folder-name">${folder.name}</span>
    <div class="gallery-actions">
        <button title="Copiază link" onclick="copyLink(${folder.id}); event.stopPropagation();">🔗</button>
        <button title="Descarcă" onclick="downloadFolder(${folder.id}); event.stopPropagation();">⬇️</button>
        <button title="Redenumește" onclick="renameFolder(${folder.id}, event)">✏️</button>
        <button title="Mută" onclick="moveFolder(${folder.id})">📂</button>
        <button title="Șterge" onclick="deleteFolder(${folder.id}, event)">🗑️</button>
    </div>
`;
        div.onclick = (e) => {
            if (!e.target.matches('button')) navigateToFolder(folder.id, folder.name);
        };


        container.appendChild(div);
    });
}


function renderFiles(files) {
    const container = document.getElementById("file-container");
    container.innerHTML = "";

    files.forEach(file => {
        const div = document.createElement("div");
        div.className = "file-card";

        const filename = file.filename;
        const fileUrl = `/s/${file.uniqueId}`;
        let content = "";

        if (file.fileType.startsWith("image/")) {
            content = `<img src="${fileUrl}" alt="${filename}">`;
        } else if (file.fileType.startsWith("video/")) {
            content = `
                <video style="width: 100%; max-height: 100px; border-radius: 8px;" muted>
                    <source src="${fileUrl}" type="${file.fileType}">
                </video>
            `;
        } else if (file.fileType === "application/pdf") {
            content = `<img src="/img/icons/pdf.png" alt="PDF" style="width: 100px">`;
        } else {
            content = `<img src="/img/icons/file.png" alt="File" style="width: 100px">`;
        }

        div.innerHTML = `
            ${content}
            <span>${filename}</span>
        `;

        // 🔥 Această funcție deschide galeria pentru orice fișier
        div.addEventListener("click", function (e) {
            // Ignoră click pe elemente interne (ex. butoane play)
            if (["VIDEO", "SOURCE", "BUTTON"].includes(e.target.tagName)) return;
            window.open(`/preview.html?id=${file.galleryId}`, "_blank");
        });

        container.appendChild(div);
    });
}




function deleteFolder(id, e) {
    e.stopPropagation();
    if (!confirm("Sigur ștergi mapa?")) return;
    fetch(`/api/user/folders/${id}`, { method: "DELETE", credentials: "include" })
        .then(() => loadFoldersAndFiles());
}

function renameFolder(id, e) {
    e.stopPropagation();
    const newName = prompt("Introdu noul nume:");
    if (!newName) return;
    fetch(`/api/user/folders/${id}`, {
        method: "PUT",
        headers: { 'Content-Type': 'text/plain' },
        credentials: "include",
        body: newName
    }).then(() => loadFoldersAndFiles());
}

function moveGallery(galleryId) {
    const folderId = prompt("Introdu ID-ul noii mape unde vrei să muți:");
    if (!folderId) return;

    fetch(`/api/admin/galleries/${galleryId}/move?newFolderId=${folderId}`, {
        method: "PUT",
        credentials: "include"
    })
        .then(res => res.text())
        .then(msg => {
            alert("✅ " + msg);
            loadGalleries();
        })
        .catch(err => alert("❌ " + err.message));
}

function copyLink(folderId) {
    const url = `${window.location.origin}/preview.html?folderId=${folderId}`;
    navigator.clipboard.writeText(url)
        .then(() => alert("🔗 Link copiat: " + url))
        .catch(() => alert("❌ Nu s-a putut copia linkul."));
}

function downloadFolder(id) {
    window.open(`/api/user/folders/${id}/download-zip`, "_blank");
}

function renameFolder(id, e) {
    e.stopPropagation();
    const newName = prompt("📝 Introdu noul nume pentru folder:");
    if (!newName) return;

    fetch(`/api/user/folders/${id}`, {
        method: "PUT",
        headers: { 'Content-Type': 'text/plain' },
        credentials: "include",
        body: newName
    }).then(res => {
        if (res.ok) {
            alert("✅ Nume modificat!");
            loadFoldersAndFiles(); // actualizează lista
        } else alert("❌ Eroare la redenumire.");
    });
}

function getThumbnail(file) {
    const ext = file.filename.split('.').pop().toLowerCase();
    if (['mp4', 'mov', 'avi'].includes(ext)) {
        return '/img/video-icon.png'; // ai copiat acest fișier din /mnt/data/
    }
    return `/img/thumbs/${file.uniqueId}`;
}


function moveFolder(id) {
    const targetId = prompt("📂 Introdu ID-ul folderului unde vrei să muți:");
    if (!targetId) return;

    fetch(`/api/user/folders/${id}/move?targetId=${targetId}`, {
        method: "POST",
        credentials: "include"
    })
        .then(res => {
            if (res.ok) {
                alert("✅ Folder mutat!");
                loadFoldersAndFiles();
            } else {
                alert("❌ Eroare la mutare.");
            }
        });
}
function deleteFolder(id, e) {
    e.stopPropagation();
    if (!confirm("🗑️ Ești sigur că vrei să ștergi acest folder?")) return;

    fetch(`/api/user/folders/${id}`, {
        method: "DELETE",
        credentials: "include"
    }).then(res => {
        if (res.ok) {
            alert("✅ Șters cu succes!");
            loadFoldersAndFiles();
        } else alert("❌ Eroare la ștergere.");
    });
}

function createFolder() {
    const name = prompt("Introdu numele noii mape:");
    if (!name) return;
    fetch("/api/user/folders", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ name: name, parentId: currentFolderId })
    })
        .then(res => {
            if (!res.ok) throw new Error("Eroare la creare mapă");
            return res.text();
        })
        .then(() => loadFoldersAndFiles())
        .catch(err => alert("❌ " + err.message));
}
function toggleTable(type) {
    document.getElementById("table-admin").classList.add("hidden");
    document.getElementById("table-user").classList.add("hidden");
    document.getElementById("table-anonim").classList.add("hidden");

    if (type === "admin") {
        document.getElementById("table-admin").classList.remove("hidden");
    } else if (type === "user") {
        document.getElementById("table-user").classList.remove("hidden");
    } else if (type === "anonim") {
        document.getElementById("table-anonim").classList.remove("hidden");
    }
}

