# 🚀 DriveStudio.md

**DriveStudio** este o platformă web completă pentru încărcarea, gestionarea și partajarea fișierelor, inspirată din modelul fex.net. Include suport pentru utilizatori autentificați și administratori, sistem de galerii, foldere ierarhice, descărcare ZIP, și interfață intuitivă.

---

## 🔧 Tehnologii utilizate

- **Backend**: Java 17, Spring Boot 3, Spring Security
- **Frontend**: HTML5, CSS3, JavaScript
- **Bază de date**: MariaDB
- **Stocare fișiere**: Local file system
- **Autentificare**: Pe bază de sesiune (fără JWT)
- **Deploy**: Docker + Docker Compose, Synology Server
- **Altele**: LightGallery, Bootstrap, FontAwesome

---

## 📦 Funcționalități principale

### 🔐 Autentificare:
- Login și înregistrare
- Roluri: `USER`, `ADMIN`
- Sesiune HTTP, fără token

### 📂 Management fișiere:
- Upload multiple fișiere simultan
- Limită de 2GB pentru utilizatori
- Galerie per folder / galerie
- Descărcare fișiere individual sau ca arhivă ZIP
- Link unic pentru accesare publică (preview)

### 🛠️ Panou Admin:
- Vizualizare toate galeriile
- Gestionare utilizatori
- Setare memorie maximă
- Ștergere galerii / utilizatori
- Mape ierarhice & navigare cu breadcrumb

### 🎨 Interfață modernă:
- Mod zi/noapte
- Galerie lightbox
- UI responsive inspirat din fex.net

---

## 🧪 Cum rulez aplicația local?

### 🔹 Variante disponibile:

#### A) Rulare directă din IDE (IntelliJ):

1. Clonează repo-ul:
   ```bash
   git clone https://github.com/sturzaveaceslav/drivestudio-full.git
