package com.GemmaGuardian.securitymonitor.domain.model

import kotlinx.datetime.Instant

data class SecurityStats(
    val totalAlerts: Int,
    val criticalAlerts: Int,
    val highAlerts: Int,
    val mediumAlerts: Int,
    val lowAlerts: Int,
    val lastAlertTime: Instant?
)
