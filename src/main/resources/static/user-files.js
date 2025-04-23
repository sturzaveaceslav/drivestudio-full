function formatDate(dateStr) {
    const d = new Date(dateStr);
    return d.toLocaleString();
}

async function fetchFiles() {
    const res = await fetch("/api/user/files");
    const files = await res.json();
    const list = document.getElementById("fileList");
    list.innerHTML = "";

    files.forEach(file => {
        const item = document.createElement("div");
        item.className = "file-item";
        item.innerHTML = `
            <div><strong>${file.filename}</strong><br>Data: ${formatDate(file.uploadDate)}</div>
            <div class="file-actions">
                <button class="deschide" onclick="window.open('/preview.html?id=${file.galleryId}')">Deschide</button>
                <button class="copiaza" onclick="copyLink('${file.uniqueId}')">Copiază</button>
                <button class="sterge" onclick="deleteFile(${file.id})">Șterge</button>
            </div>
        `;
        list.appendChild(item);
    });
}

async function fetchStorage() {
    const res = await fetch("/api/user/space");
    const data = await res.json();
    const used = (data.used / (1024 * 1024)).toFixed(2);
    const max = (data.max / (1024 * 1024)).toFixed(2);
    document.getElementById("storage-info").innerText = `Spațiu folosit: ${used} MB / ${max} MB`;
}

function copyLink(id) {
    const link = window.location.origin + "/s/" + id;
    navigator.clipboard.writeText(link);
    alert("Link copiat: " + link);
}

async function deleteFile(id) {
    if (!confirm("Ești sigur că vrei să ștergi fișierul?")) return;
    await fetch("/api/user/files/" + id, { method: "DELETE" });
    fetchFiles();
    fetchStorage();
}

async function uploadFiles() {
    const files = document.getElementById("fileInput").files;
    const customName = document.getElementById("customName").value;

    const formData = new FormData();
    for (let file of files) formData.append("files", file);
    formData.append("customName", customName);

    const xhr = new XMLHttpRequest();
    xhr.open("POST", "/upload", true);
    xhr.upload.onprogress = function(e) {
        if (e.lengthComputable) {
            let percent = (e.loaded / e.total * 100).toFixed(0);
            document.getElementById("progressFill").style.width = percent + "%";
            document.getElementById("progressFill").innerText = percent + "%";
        }
    };
    xhr.onload = function() {
        if (xhr.status === 200) {
            alert("Fișiere încărcate cu succes!");
            document.getElementById("progressFill").style.width = "0%";
            fetchFiles();
            fetchStorage();
        } else {
            alert("Eroare la încărcare: " + xhr.responseText);
        }
    };
    xhr.send(formData);
}

fetchFiles();
fetchStorage();
