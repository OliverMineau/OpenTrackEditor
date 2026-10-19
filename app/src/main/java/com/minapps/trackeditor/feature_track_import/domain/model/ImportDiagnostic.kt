package com.minapps.trackeditor.feature_track_import.domain.model

enum class ImportDiagnosticScope { DOCUMENT, TRACK }

enum class ImportDiagnosticKind { DATA, STRUCTURAL }

data class ImportDiagnostic(
    val trackId: Int?,
    val trackName: String?,
    val message: String,
    val line: Int? = null,
    val column: Int? = null,
    val scope: ImportDiagnosticScope = ImportDiagnosticScope.TRACK,
    val kind: ImportDiagnosticKind = ImportDiagnosticKind.DATA
)