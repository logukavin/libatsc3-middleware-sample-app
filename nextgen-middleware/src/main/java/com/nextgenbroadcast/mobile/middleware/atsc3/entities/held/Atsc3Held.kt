package com.nextgenbroadcast.mobile.middleware.atsc3.entities.held

import java.time.ZonedDateTime

class Atsc3Held(
        val packages: List<Atsc3HeldPackage>
) {
    fun findActivePackage(serviceId: Int): Atsc3HeldPackage? {
        return packages.firstOrNull { pkg ->
            isValid(pkg) && pkg.coupledServices?.contains(serviceId) ?: false
        }
    }

    private fun isValid(pkg: Atsc3HeldPackage): Boolean {
        val now: ZonedDateTime by lazy { ZonedDateTime.now() }
        //TODO: check requiredCapabilities
        return with(pkg) {
            !appContextId.isNullOrEmpty()
                    && !bcastEntryPackageUrl.isNullOrEmpty()
                    && (!bcastEntryPageUrl.isNullOrEmpty() || !bbandEntryPageUrl.isNullOrEmpty())
                    && (validFrom == null || now.isAfter(validFrom))
                    && (validUntil == null || now.isBefore(validUntil))
                    && !coupledServices.isNullOrEmpty()
        }
    }
}