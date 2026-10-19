package lat.pam.laporkampus

import lat.pam.laporkampus.data.model.Report
import lat.pam.laporkampus.data.model.ReportStatus
import lat.pam.laporkampus.util.StatusTransitionRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test komprehensif untuk [StatusTransitionRules] (Sprint 6).
 * Memastikan alur status laporan: DITERIMA -> DIVERIFIKASI -> DIKERJAKAN -> SELESAI, plus DITOLAK.
 */
class StatusTransitionRulesTest {

    @Test
    fun fromDiterima_onlyAllowsDiverifikasiOrDitolak() {
        val next = StatusTransitionRules.nextValidStatuses(ReportStatus.DITERIMA)
        assertEquals(setOf(ReportStatus.DIVERIFIKASI, ReportStatus.DITOLAK), next)

        assertTrue(StatusTransitionRules.canTransition(ReportStatus.DITERIMA, ReportStatus.DIVERIFIKASI))
        assertTrue(StatusTransitionRules.canTransition(ReportStatus.DITERIMA, ReportStatus.DITOLAK))

        assertFalse(StatusTransitionRules.canTransition(ReportStatus.DITERIMA, ReportStatus.DITERIMA))
        assertFalse(StatusTransitionRules.canTransition(ReportStatus.DITERIMA, ReportStatus.DIKERJAKAN))
        assertFalse(StatusTransitionRules.canTransition(ReportStatus.DITERIMA, ReportStatus.SELESAI))
    }

    @Test
    fun fromDiverifikasi_onlyAllowsDikerjakanOrDitolak() {
        val next = StatusTransitionRules.nextValidStatuses(ReportStatus.DIVERIFIKASI)
        assertEquals(setOf(ReportStatus.DIKERJAKAN, ReportStatus.DITOLAK), next)

        assertTrue(StatusTransitionRules.canTransition(ReportStatus.DIVERIFIKASI, ReportStatus.DIKERJAKAN))
        assertTrue(StatusTransitionRules.canTransition(ReportStatus.DIVERIFIKASI, ReportStatus.DITOLAK))

        assertFalse(StatusTransitionRules.canTransition(ReportStatus.DIVERIFIKASI, ReportStatus.DITERIMA))
        assertFalse(StatusTransitionRules.canTransition(ReportStatus.DIVERIFIKASI, ReportStatus.DIVERIFIKASI))
        assertFalse(StatusTransitionRules.canTransition(ReportStatus.DIVERIFIKASI, ReportStatus.SELESAI))
    }

    @Test
    fun fromDikerjakan_onlyAllowsSelesai() {
        val next = StatusTransitionRules.nextValidStatuses(ReportStatus.DIKERJAKAN)
        assertEquals(setOf(ReportStatus.SELESAI), next)

        assertTrue(StatusTransitionRules.canTransition(ReportStatus.DIKERJAKAN, ReportStatus.SELESAI))

        assertFalse(StatusTransitionRules.canTransition(ReportStatus.DIKERJAKAN, ReportStatus.DITERIMA))
        assertFalse(StatusTransitionRules.canTransition(ReportStatus.DIKERJAKAN, ReportStatus.DIVERIFIKASI))
        assertFalse(StatusTransitionRules.canTransition(ReportStatus.DIKERJAKAN, ReportStatus.DITOLAK))
    }

    @Test
    fun terminalStatuses_haveNoOutgoingTransitions() {
        assertTrue(StatusTransitionRules.isTerminal(ReportStatus.SELESAI))
        assertTrue(StatusTransitionRules.isTerminal(ReportStatus.DITOLAK))
        assertFalse(StatusTransitionRules.isTerminal(ReportStatus.DITERIMA))
        assertFalse(StatusTransitionRules.isTerminal(ReportStatus.DIVERIFIKASI))
        assertFalse(StatusTransitionRules.isTerminal(ReportStatus.DIKERJAKAN))

        for (target in ReportStatus.entries) {
            assertFalse(StatusTransitionRules.canTransition(ReportStatus.SELESAI, target))
            assertFalse(StatusTransitionRules.canTransition(ReportStatus.DITOLAK, target))
        }
    }

    @Test
    fun countByStatus_includesAllStatusesEvenWhenEmpty() {
        val emptyCounts = StatusTransitionRules.countByStatus(emptyList())
        assertEquals(5, emptyCounts.size)
        ReportStatus.entries.forEach { status ->
            assertEquals(0, emptyCounts[status])
        }

        val reports = listOf(
            Report(id = "r1", status = ReportStatus.DITERIMA.name),
            Report(id = "r2", status = ReportStatus.DITERIMA.name),
            Report(id = "r3", status = ReportStatus.DIVERIFIKASI.name),
            Report(id = "r4", status = ReportStatus.DIKERJAKAN.name),
            Report(id = "r5", status = ReportStatus.DIKERJAKAN.name),
            Report(id = "r6", status = ReportStatus.DIKERJAKAN.name),
            Report(id = "r7", status = ReportStatus.SELESAI.name),
            Report(id = "r8", status = ReportStatus.DITOLAK.name)
        )
        val counts = StatusTransitionRules.countByStatus(reports)
        assertEquals(2, counts[ReportStatus.DITERIMA])
        assertEquals(1, counts[ReportStatus.DIVERIFIKASI])
        assertEquals(3, counts[ReportStatus.DIKERJAKAN])
        assertEquals(1, counts[ReportStatus.SELESAI])
        assertEquals(1, counts[ReportStatus.DITOLAK])
    }
}
