package com.nextgenbroadcast.mobile.middleware.atsc3.entities.held

import java.time.ZonedDateTime

class Atsc3Held(
        val packages: List<Atsc3HeldPackage>
) {
    //jjustman-2020-08-31 - copuledServices is not applicable for only one serviceID...
    fun findActivePackage(serviceId: Int): Atsc3HeldPackage? {
        return packages.firstOrNull { pkg ->
            isValid(pkg) && pkg.ParsedServiceID == serviceId
        }
    }

    private fun isValid(pkg: Atsc3HeldPackage): Boolean {
        val now: ZonedDateTime by lazy { ZonedDateTime.now() }
        //TODO: check requiredCapabilities
        //jjustman-2020-08-31 - coupledServices is not required,
        // (bcastEntryPackageUrl ^ bcastEntryPageUrl) | bbandEntryPageUrl are required

        return with(pkg) {
            !appContextId.isNullOrEmpty()
                    && ((!bcastEntryPackageUrl.isNullOrEmpty() && !bcastEntryPageUrl.isNullOrEmpty()) || !bbandEntryPageUrl.isNullOrEmpty())
                    && (validFrom == null || now.isAfter(validFrom))
                    && (validUntil == null || now.isBefore(validUntil))

        }
    }
}