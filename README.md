# ThreeDViewing_Minggu12 — Grafika Komputer

Kelanjutan aplikasi Visual Realism Minggu 11 untuk tugas Minggu 12. Java 17+, Maven, LWJGL 3, JOML, GLSL 330. Aplikasi merender sphere dengan tekstur bata PNG, pemilihan filter Nearest atau Linear + mipmap, dan bayangan real-time dengan depth FBO dua pass serta PCF 3×3.

## Jalankan di NetBeans

File → Open Project → pilih folder proyek yang berisi `pom.xml`. Gunakan JDK 17 atau lebih baru. Klik kanan proyek `ThreeDViewing_Minggu12` → Clean and Build → Run. Perlu internet saat dependensi Maven belum terunduh.

Windows PowerShell: `./run.ps1`. Pemeriksaan singkat OpenGL: `./run.ps1 --smoke`.

## Kontrol

- `T`: Nearest ↔ Linear + mipmap (bandingkan tekstur pada jarak dekat dan jauh).
- `M`: tekstur on/off.
- `1`, `2`, `3`: pilih posisi cahaya dan amati arah bayangan. Untuk memenuhi langkah mengubah kode pada modul, edit konstanta `LIGHT_POSITIONS` di `VisualRealismApp.java`, lalu jalankan ulang.
- `L`: bayangan on/off; depth pass tetap berjalan saat off.
- `B`: Phong ↔ Blinn-Phong; `F`: flat ↔ smooth; `Z`: depth test; `C`: back-face culling.
- Panah: orbit kamera; `+` / `-`: zoom; `Esc`: keluar.

Tekstur kustom `src/main/resources/textures/brick.png` dibuat oleh `generate_brick_texture.py`; file ini bisa diganti gambar milik sendiri. Bias PCF dapat diubah pada `src/main/resources/shaders/scene.frag` (nilai awal `0.006` dan `0.0008`).

Repository ini berisi **kode sumber**. Laporan dan screenshot pengujian disimpan terpisah agar data identitas mahasiswa dan berkas Word yang sedang diedit tidak terunggah tanpa sengaja.
