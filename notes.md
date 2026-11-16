
# Posisi Secara SDLC

┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│ 1. Requirmnt │──▶│ 2. Desain    │──▶│ 3. Implement │──▶│ 4. Test      │
│  / PRD draft │   │  + QUINT SPEC│   │  (refactor)  │   │  unit+replay │
└──────────────┘   └──────────────┘   └──────────────┘   └──────────────┘
│                  │                  │                  │
aturan bisnis    model & invariant    kode legacy/baru   trace Quint →
jadi invariant   dijalankan (run/     dibandingkan       test JUnit
verify) → temukan   dengan model
bug desain dini
│
┌──────────────┐   ┌──────────────┐   ┌──────────────┐           ▼
│ 8. Production│◀──│ 7. Staging   │◀──│ 6. Gerbang   │◀──│ 5. Review    │
│              │   │  + monitor   │   │  CI: quint   │   │  PR + spec   │
└──────────────┘   └──────────────┘   └──────────────┘   └──────────────┘


## Contoh Use Case untuk kasus lama

### Struktur Folder

transfer-modernisasi/
├── legacy/
│   └── src/main/java/app/TransferService.java   ← kode lama (bug)
├── fixed/
│   └── src/main/java/app/TransferService.java   ← kode baru (diperbaiki)
├── specs/
│   ├── transfer_legacy.qnt    ← model perilaku legacy
│   └── transfer_fixed.qnt     ← model desain baru
├── traces/                    ← output trace (opsional)
└── scripts/
└── quint-check.ps1        ← gerbang lokal/CI



#### Uji coba

Oke karena saya pernah bikin study case untuk uji coba yok kita buat uji coba 
