package com.swiftyprotein.data

import androidx.compose.ui.graphics.Color

data class Atom(
    val id: Int,
    val element: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val name: String = "",
    val residueName: String = ""
)

data class Bond(
    val atom1Id: Int,
    val atom2Id: Int
)

object CpkColors {
    private val colorMap = mapOf(
        "H" to Color(0xFFFFFFFF),
        "C" to Color(0xFF808080),
        "N" to Color(0xFF0000FF),
        "O" to Color(0xFFFF0000),
        "S" to Color(0xFFFFFF00),
        "P" to Color(0xFFFFA500),
        "CL" to Color(0xFF00FF00),
        "F" to Color(0xFF00FF00),
        "BR" to Color(0xFFA52A2A),
        "I" to Color(0xFF9400D3),
        "HE" to Color(0xFF00FFFF),
        "NE" to Color(0xFF00FFFF),
        "AR" to Color(0xFF00FFFF),
        "XE" to Color(0xFF00FFFF),
        "KR" to Color(0xFF00FFFF),
        "LI" to Color(0xFFB22222),
        "NA" to Color(0xFFB22222),
        "K" to Color(0xFFB22222),
        "RB" to Color(0xFFB22222),
        "CS" to Color(0xFFB22222),
        "FR" to Color(0xFFB22222),
        "BE" to Color(0xFF006400),
        "MG" to Color(0xFF006400),
        "CA" to Color(0xFF006400),
        "SR" to Color(0xFF006400),
        "BA" to Color(0xFF006400),
        "RA" to Color(0xFF006400),
        "TI" to Color(0xFF808080),
        "FE" to Color(0xFFFFA500)
    )

    fun getColor(element: String): Color {
        return colorMap[element.uppercase()] ?: Color(0xFFFF1493) // DeepPink for unknown
    }
}
