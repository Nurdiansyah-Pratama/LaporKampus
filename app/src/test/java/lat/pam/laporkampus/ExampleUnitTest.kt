package lat.pam.laporkampus

import lat.pam.laporkampus.data.model.ReportStatus
import lat.pam.laporkampus.util.StatusTransitionRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun validStatusTransitions_areAllowed() {
        assertTrue(StatusTransitionRules.canTransition(ReportStatus.DITERIMA, ReportStatus.DIVERIFIKASI))
        assertTrue(StatusTransitionRules.canTransition(ReportStatus.DITERIMA, ReportStatus.DITOLAK))
        assertTrue(StatusTransitionRules.canTransition(ReportStatus.DIVERIFIKASI, ReportStatus.DIKERJAKAN))
        assertTrue(StatusTransitionRules.canTransition(ReportStatus.DIKERJAKAN, ReportStatus.SELESAI))
    }

    @Test
    fun invalidStatusTransitions_areBlocked() {
        assertFalse(StatusTransitionRules.canTransition(ReportStatus.DITERIMA, ReportStatus.SELESAI))
        assertFalse(StatusTransitionRules.canTransition(ReportStatus.SELESAI, ReportStatus.DIKERJAKAN))
        assertFalse(StatusTransitionRules.canTransition(ReportStatus.DITOLAK, ReportStatus.DIVERIFIKASI))
    }
}