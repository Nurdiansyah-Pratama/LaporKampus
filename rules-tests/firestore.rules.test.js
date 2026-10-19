const fs = require("fs");
const path = require("path");
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");
const {
  doc,
  getDoc,
  setDoc,
  updateDoc,
  deleteDoc,
  Timestamp,
} = require("firebase/firestore");

const PROJECT_ID = "laporkampus-emulator-test";

describe("LaporKampus Firestore Security Rules", () => {
  let testEnv;

  before(async () => {
    const rulesPath = path.resolve(__dirname, "../firestore.rules");
    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        rules: fs.readFileSync(rulesPath, "utf8"),
        host: "127.0.0.1",
        port: 8080,
      },
    });
  });

  beforeEach(async () => {
    await testEnv.clearFirestore();

    // Seed akun staf (ADMIN, TEKNISI, SATGAS) dan MAHASISWA menggunakan konteks bypass rules
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await setDoc(doc(db, "users/uid-mhs-1"), {
        id: "uid-mhs-1",
        nama: "Budi Mahasiswa",
        email: "budi@kampus.ac.id",
        peran: "MAHASISWA",
      });
      await setDoc(doc(db, "users/uid-admin-1"), {
        id: "uid-admin-1",
        nama: "Admin Kampus",
        email: "admin@kampus.ac.id",
        peran: "ADMIN",
      });
      await setDoc(doc(db, "users/uid-tech-1"), {
        id: "uid-tech-1",
        nama: "Andi Teknisi",
        email: "teknisi@kampus.ac.id",
        peran: "TEKNISI",
      });
      await setDoc(doc(db, "users/uid-satgas-1"), {
        id: "uid-satgas-1",
        nama: "Petugas Satgas",
        email: "satgas@kampus.ac.id",
        peran: "SATGAS",
      });
    });
  });

  after(async () => {
    if (testEnv) {
      await testEnv.cleanup();
    }
  });

  it("Aturan #1: Registrasi mandiri HANYA mengizinkan peran MAHASISWA dan menolak ADMIN/TEKNISI/SATGAS", async () => {
    const userBaruCtx = testEnv.authenticatedContext("uid-mhs-baru");
    const dbBaru = userBaruCtx.firestore();

    // Mencoba daftar sendiri sebagai ADMIN harus ditolak
    await assertFails(
      setDoc(doc(dbBaru, "users/uid-mhs-baru"), {
        id: "uid-mhs-baru",
        nama: "Peretas",
        email: "peretas@kampus.ac.id",
        peran: "ADMIN",
      })
    );

    // Daftar sendiri sebagai MAHASISWA diizinkan
    await assertSucceeds(
      setDoc(doc(dbBaru, "users/uid-mhs-baru"), {
        id: "uid-mhs-baru",
        nama: "Mahasiswa Baru",
        email: "mhsbaru@kampus.ac.id",
        peran: "MAHASISWA",
      })
    );

    // Mencoba eskalasi peran sendiri dari MAHASISWA ke SATGAS saat update harus ditolak
    await assertFails(
      updateDoc(doc(dbBaru, "users/uid-mhs-baru"), {
        peran: "SATGAS",
      })
    );
  });

  it("Aturan #2: Laporan dengan isSensitive = true HANYA boleh dibaca oleh pelapornya sendiri dan SATGAS", async () => {
    const now = Timestamp.now();
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await setDoc(doc(db, "reports/rep-sensitif-1"), {
        id: "rep-sensitif-1",
        reporterId: "uid-mhs-1",
        isAnonymous: true,
        isSensitive: true,
        kategori: "Kekerasan / Pelecehan / Perundungan (Sensitif)",
        deskripsi: "Laporan sensitif rahasia untuk Satgas.",
        photoUrls: [],
        lat: -6.8915,
        lng: 107.6107,
        namaLokasi: "Gedung A",
        status: "DITERIMA",
        prioritas: "DARURAT",
        assignedTo: "",
        createdAt: now,
        updatedAt: now,
      });
    });

    const mhs1Db = testEnv.authenticatedContext("uid-mhs-1").firestore();
    const satgasDb = testEnv.authenticatedContext("uid-satgas-1").firestore();
    const adminDb = testEnv.authenticatedContext("uid-admin-1").firestore();
    const techDb = testEnv.authenticatedContext("uid-tech-1").firestore();

    // Pelapor sendiri & SATGAS diizinkan membaca laporan sensitif
    await assertSucceeds(getDoc(doc(mhs1Db, "reports/rep-sensitif-1")));
    await assertSucceeds(getDoc(doc(satgasDb, "reports/rep-sensitif-1")));

    // ADMIN & TEKNISI dilarang keras membaca laporan sensitif
    await assertFails(getDoc(doc(adminDb, "reports/rep-sensitif-1")));
    await assertFails(getDoc(doc(techDb, "reports/rep-sensitif-1")));
  });

  it("Aturan Transisi Status & Riwayat: menegakkan DITERIMA -> DIVERIFIKASI -> DIKERJAKAN -> SELESAI dan history bersifat append-only", async () => {
    const now = Timestamp.now();
    const mhs1Db = testEnv.authenticatedContext("uid-mhs-1").firestore();
    const adminDb = testEnv.authenticatedContext("uid-admin-1").firestore();

    // Mahasiswa membuat laporan dengan status awal DITERIMA
    await assertSucceeds(
      setDoc(doc(mhs1Db, "reports/rep-1"), {
        id: "rep-1",
        reporterId: "uid-mhs-1",
        isAnonymous: false,
        isSensitive: false,
        kategori: "Kelistrikan & Pencahayaan",
        deskripsi: "Lampu koridor gedung C padam sejak kemarin malam.",
        photoUrls: [],
        lat: -6.8915,
        lng: 107.6107,
        namaLokasi: "Gedung C Lt 1",
        status: "DITERIMA",
        prioritas: "SEDANG",
        assignedTo: "",
        createdAt: now,
        updatedAt: now,
      })
    );

    // Admin tidak boleh melompat langsung dari DITERIMA ke SELESAI
    await assertFails(
      updateDoc(doc(adminDb, "reports/rep-1"), {
        status: "SELESAI",
        updatedAt: Timestamp.now(),
      })
    );

    // Admin memverifikasi DITERIMA -> DIVERIFIKASI
    await assertSucceeds(
      updateDoc(doc(adminDb, "reports/rep-1"), {
        status: "DIVERIFIKASI",
        prioritas: "TINGGI",
        assignedTo: "Andi Teknisi",
        updatedAt: Timestamp.now(),
      })
    );

    // Tambahkan catatan riwayat dan pastikan tidak dapat dihapus (append-only)
    await assertSucceeds(
      setDoc(doc(adminDb, "reports/rep-1/history/hist-1"), {
        status: "DIVERIFIKASI",
        catatan: "Diverifikasi oleh Admin.",
        oleh: "Admin Kampus",
        waktu: Timestamp.now(),
      })
    );
    await assertFails(deleteDoc(doc(adminDb, "reports/rep-1/history/hist-1")));
  });

  it("Aturan Rating: hanya pelapor asli yang dapat memberi nilai 1..5 setelah laporan berstatus SELESAI", async () => {
    const now = Timestamp.now();
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await setDoc(doc(db, "reports/rep-belum"), {
        id: "rep-belum",
        reporterId: "uid-mhs-1",
        isAnonymous: false,
        isSensitive: false,
        kategori: "Kebersihan & Taman",
        deskripsi: "Tempat sampah penuh di taman tengah.",
        photoUrls: [],
        lat: null,
        lng: null,
        namaLokasi: "Taman Tengah",
        status: "DIKERJAKAN",
        prioritas: "SEDANG",
        assignedTo: "Andi Teknisi",
        createdAt: now,
        updatedAt: now,
      });
      await setDoc(doc(db, "reports/rep-selesai"), {
        id: "rep-selesai",
        reporterId: "uid-mhs-1",
        isAnonymous: false,
        isSensitive: false,
        kategori: "Kebersihan & Taman",
        deskripsi: "Tempat sampah penuh di taman tengah.",
        photoUrls: [],
        lat: null,
        lng: null,
        namaLokasi: "Taman Tengah",
        status: "SELESAI",
        prioritas: "SEDANG",
        assignedTo: "Andi Teknisi",
        createdAt: now,
        updatedAt: now,
      });
    });

    const mhs1Db = testEnv.authenticatedContext("uid-mhs-1").firestore();

    // Gagal jika laporan belum SELESAI
    await assertFails(
      setDoc(doc(mhs1Db, "ratings/rep-belum"), {
        reportId: "rep-belum",
        nilai: 5,
        komentar: "Coba nilai sebelum selesai",
      })
    );

    // Gagal jika nilai di luar rentang 1..5
    await assertFails(
      setDoc(doc(mhs1Db, "ratings/rep-selesai"), {
        reportId: "rep-selesai",
        nilai: 6,
        komentar: "Nilai tidak valid",
      })
    );

    // Berhasil jika laporan berstatus SELESAI dan nilai 1..5 oleh pelapor asli
    await assertSucceeds(
      setDoc(doc(mhs1Db, "ratings/rep-selesai"), {
        reportId: "rep-selesai",
        nilai: 5,
        komentar: "Sangat cepat ditangani, terima kasih!",
      })
    );
  });
});
