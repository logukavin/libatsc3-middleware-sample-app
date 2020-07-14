package org.ngbp.libatsc3.entities.held

data class Held (
    var appContextId: String? = null,
    var bcastEntryPackageUrl: String? = null,
    var bcastEntryPageUrl: String? = null,
    var coupledServices: Int = 0,
    var appRendering: Boolean = false
)