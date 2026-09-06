package com.swiftyprotein.data

// Parser to extract atoms and bonds from a CIF chemical file
class CifParser {

    // Helper data structure to temporarily hold both experimental and ideal coordinates for an atom
    private data class RawAtom(
        val idStr: String,
        val element: String,
        val modelX: Float?,
        val modelY: Float?,
        val modelZ: Float?,
        val idealX: Float?,
        val idealY: Float?,
        val idealZ: Float?
    )

    // Parse the raw text content of a CIF file and return the list of atoms and bonds
    fun parse(cifContent: String): Pair<List<Atom>, List<Bond>> {
        val rawAtoms = mutableListOf<RawAtom>()
        val bonds = mutableListOf<Bond>()
        val atomIdToIndex = mutableMapOf<String, Int>()
        val singleFields = mutableMapOf<String, String>()
        
        val lines = cifContent.lines()
        var inAtomLoop = false
        var inBondLoop = false
        var inLoopHeaders = false
        val currentLoopFields = mutableListOf<String>()

        // Read line by line to locate loop headers and data sections
        lines.forEach { line ->
            val trimmed = line.trim()
            
            // Skip comments and empty lines, and exit current loop when a comment separator '#' is hit
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                if (trimmed.startsWith("#")) {
                    inAtomLoop = false
                    inBondLoop = false
                }
                return@forEach
            }

            // Store single value fields outside of loops
            if (trimmed.startsWith("_") && !inLoopHeaders && !inAtomLoop && !inBondLoop) {
                val parts = splitCifLine(trimmed)
                if (parts.size >= 2) {
                    singleFields[parts[0]] = parts.drop(1).joinToString(" ").replace("\"", "").replace("'", "")
                }
                return@forEach
            }

            // Detect the start of a loop block
            if (trimmed.startsWith("loop_")) {
                inAtomLoop = false
                inBondLoop = false
                inLoopHeaders = true
                currentLoopFields.clear()
                return@forEach
            }
            
            // Collect the column names for the current loop
            if (inLoopHeaders && trimmed.startsWith("_")) {
                currentLoopFields.add(trimmed)
                if (trimmed.startsWith("_chem_comp_atom.")) inAtomLoop = true
                if (trimmed.startsWith("_chem_comp_bond.")) inBondLoop = true
                return@forEach
            }
            
            // Finish reading column names when data rows start
            if (inLoopHeaders && !trimmed.startsWith("_")) {
                inLoopHeaders = false
            }

            // Parse atom data rows in the atom loop
            if (inAtomLoop) {
                val parts = splitCifLine(trimmed)
                val atomIdIdx = currentLoopFields.indexOf("_chem_comp_atom.atom_id")
                val typeSymbolIdx = currentLoopFields.indexOf("_chem_comp_atom.type_symbol")
                
                val xIdx = currentLoopFields.indexOf("_chem_comp_atom.model_Cartn_x")
                val yIdx = currentLoopFields.indexOf("_chem_comp_atom.model_Cartn_y")
                val zIdx = currentLoopFields.indexOf("_chem_comp_atom.model_Cartn_z")

                val xIdealIdx = currentLoopFields.indexOf("_chem_comp_atom.pdbx_model_Cartn_x_ideal")
                val yIdealIdx = currentLoopFields.indexOf("_chem_comp_atom.pdbx_model_Cartn_y_ideal")
                val zIdealIdx = currentLoopFields.indexOf("_chem_comp_atom.pdbx_model_Cartn_z_ideal")

                if (atomIdIdx != -1) {
                    val idStr = parts.getOrNull(atomIdIdx) ?: "UNK"
                    // Ignore bogus atom IDs like '#' if encountered in malformed data
                    if (idStr == "#") return@forEach

                    val element = parts.getOrNull(typeSymbolIdx)?.replace("\"", "")?.replace("'", "") ?: "C"
                    
                    val modelX = if (xIdx != -1) parts.getOrNull(xIdx)?.toFloatOrNull() else null
                    val modelY = if (yIdx != -1) parts.getOrNull(yIdx)?.toFloatOrNull() else null
                    val modelZ = if (zIdx != -1) parts.getOrNull(zIdx)?.toFloatOrNull() else null

                    val idealX = if (xIdealIdx != -1) parts.getOrNull(xIdealIdx)?.toFloatOrNull() else null
                    val idealY = if (yIdealIdx != -1) parts.getOrNull(yIdealIdx)?.toFloatOrNull() else null
                    val idealZ = if (zIdealIdx != -1) parts.getOrNull(zIdealIdx)?.toFloatOrNull() else null

                    // Immediately register the atom ID mapping so bond lookups work during the file read
                    atomIdToIndex[idStr] = rawAtoms.size
                    rawAtoms.add(RawAtom(idStr, element, modelX, modelY, modelZ, idealX, idealY, idealZ))
                }
            }
            
            // Parse bond connectivity rows in the bond loop
            if (inBondLoop) {
                val parts = splitCifLine(trimmed)
                val id1Idx = currentLoopFields.indexOf("_chem_comp_bond.atom_id_1")
                val id2Idx = currentLoopFields.indexOf("_chem_comp_bond.atom_id_2")
                
                if (id1Idx != -1 && id2Idx != -1) {
                    val id1 = parts.getOrNull(id1Idx)
                    val id2 = parts.getOrNull(id2Idx)
                    
                    if (id1 != null && id2 != null) {
                        val idx1 = atomIdToIndex[id1]
                        val idx2 = atomIdToIndex[id2]
                        
                        if (idx1 != null && idx2 != null) {
                            bonds.add(Bond(idx1, idx2))
                        }
                    }
                }
            }
        }

        // Determine whether all atoms have complete experimental model coordinates
        val hasCompleteModelCoords = rawAtoms.isNotEmpty() && rawAtoms.all { 
            it.modelX != null && it.modelY != null && it.modelZ != null 
        }

        val atoms = mutableListOf<Atom>()

        // Convert raw atoms to final Atom objects using a single consistent coordinate frame
        rawAtoms.forEachIndexed { index, raw ->
            val x: Float
            val y: Float
            val z: Float

            if (hasCompleteModelCoords) {
                // All atoms have valid experimental coordinates
                x = raw.modelX!!
                y = raw.modelY!!
                z = raw.modelZ!!
            } else {
                // If any atom has '?' for experimental coordinates, use ideal coordinates for all atoms
                x = raw.idealX ?: raw.modelX ?: 0f
                y = raw.idealY ?: raw.modelY ?: 0f
                z = raw.idealZ ?: raw.modelZ ?: 0f
            }

            atoms.add(Atom(index, raw.element, x, y, z, raw.idStr, ""))
        }

        // Fallback for single atom files if no loop data was found
        if (atoms.isEmpty()) {
            val element = singleFields["_chem_comp_atom.type_symbol"]?.replace("\"", "")?.replace("'", "") ?: "C"
            var x = singleFields["_chem_comp_atom.model_Cartn_x"]?.toFloatOrNull()
            var y = singleFields["_chem_comp_atom.model_Cartn_y"]?.toFloatOrNull()
            var z = singleFields["_chem_comp_atom.model_Cartn_z"]?.toFloatOrNull()

            if (x == null) x = singleFields["_chem_comp_atom.pdbx_model_Cartn_x_ideal"]?.toFloatOrNull()
            if (y == null) y = singleFields["_chem_comp_atom.pdbx_model_Cartn_y_ideal"]?.toFloatOrNull()
            if (z == null) z = singleFields["_chem_comp_atom.pdbx_model_Cartn_z_ideal"]?.toFloatOrNull()

            val id = singleFields["_chem_comp_atom.atom_id"] ?: "UNK"
            
            if (x != null && y != null && z != null) {
                atoms.add(Atom(0, element, x, y, z, id, ""))
            }
        }
        
        return Pair(atoms, bonds)
    }

    // Split a CIF text line into tokens while respecting quoted strings
    private fun splitCifLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var quoteChar = ' '

        for (char in line) {
            if (inQuotes) {
                if (char == quoteChar) {
                    inQuotes = false
                } else {
                    current.append(char)
                }
            } else {
                if (char == '"' || char == '\'') {
                    inQuotes = true
                    quoteChar = char
                } else if (char.isWhitespace()) {
                    if (current.isNotEmpty()) {
                        result.add(current.toString())
                        current.clear()
                    }
                } else {
                    current.append(char)
                }
            }
        }
        if (current.isNotEmpty()) result.add(current.toString())
        return result
    }
}
