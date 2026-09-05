package com.swiftyprotein.data

class CifParser {
    fun parse(cifContent: String): Pair<List<Atom>, List<Bond>> {
        val atoms = mutableListOf<Atom>()
        val bonds = mutableListOf<Bond>()
        
        val lines = cifContent.lines()
        val atomIdToIndex = mutableMapOf<String, Int>()
        
        // Single field storage for non-loop data
        val singleFields = mutableMapOf<String, String>()
        
        var inAtomLoop = false
        var inBondLoop = false
        
        var currentLoopFields = mutableListOf<String>()
        var inLoopHeaders = false

        lines.forEach { line ->
            val trimmed = line.trim()
            
            // Handle single fields
            if (trimmed.startsWith("_") && !inLoopHeaders && !inAtomLoop && !inBondLoop) {
                val parts = splitCifLine(trimmed)
                if (parts.size >= 2) {
                    singleFields[parts[0]] = parts.drop(1).joinToString(" ").replace("\"", "").replace("'", "")
                }
                return@forEach
            }

            if (trimmed.startsWith("loop_")) {
                inAtomLoop = false
                inBondLoop = false
                inLoopHeaders = true
                currentLoopFields.clear()
                return@forEach
            }
            
            if (inLoopHeaders && trimmed.startsWith("_")) {
                currentLoopFields.add(trimmed)
                if (trimmed.startsWith("_chem_comp_atom.")) inAtomLoop = true
                if (trimmed.startsWith("_chem_comp_bond.")) inBondLoop = true
                return@forEach
            }
            
            if (inLoopHeaders && !trimmed.startsWith("_")) {
                inLoopHeaders = false
            }

            if (inAtomLoop && trimmed.isNotEmpty()) {
                val parts = splitCifLine(trimmed)
                if (parts.size >= currentLoopFields.size) {
                    val atomIdIdx = currentLoopFields.indexOf("_chem_comp_atom.atom_id")
                    val typeSymbolIdx = currentLoopFields.indexOf("_chem_comp_atom.type_symbol")
                    val xIdx = currentLoopFields.indexOf("_chem_comp_atom.model_Cartn_x")
                    val yIdx = currentLoopFields.indexOf("_chem_comp_atom.model_Cartn_y")
                    val zIdx = currentLoopFields.indexOf("_chem_comp_atom.model_Cartn_z")
                    
                    if (atomIdIdx != -1 && typeSymbolIdx != -1 && xIdx != -1 && yIdx != -1 && zIdx != -1) {
                        val idStr = parts[atomIdIdx]
                        val element = parts[typeSymbolIdx].replace("\"", "").replace("'", "")
                        val x = parts[xIdx].toFloat()
                        val y = parts[yIdx].toFloat()
                        val z = parts[zIdx].toFloat()
                        
                        val index = atoms.size
                        atomIdToIndex[idStr] = index
                        atoms.add(Atom(index, element, x, y, z, idStr, ""))
                    }
                }
            }
            
            if (inBondLoop && trimmed.isNotEmpty()) {
                val parts = splitCifLine(trimmed)
                if (parts.size >= currentLoopFields.size) {
                    val id1Idx = currentLoopFields.indexOf("_chem_comp_bond.atom_id_1")
                    val id2Idx = currentLoopFields.indexOf("_chem_comp_bond.atom_id_2")
                    
                    if (id1Idx != -1 && id2Idx != -1) {
                        val id1 = parts[id1Idx]
                        val id2 = parts[id2Idx]
                        
                        val idx1 = atomIdToIndex[id1]
                        val idx2 = atomIdToIndex[id2]
                        
                        if (idx1 != null && idx2 != null) {
                            bonds.add(Bond(idx1, idx2))
                        }
                    }
                }
            }
        }
        
        // If no atoms found in loops, try to find a single atom defined by fields
        if (atoms.isEmpty()) {
            val element = singleFields["_chem_comp_atom.type_symbol"]
            val x = singleFields["_chem_comp_atom.model_Cartn_x"]?.toFloatOrNull()
            val y = singleFields["_chem_comp_atom.model_Cartn_y"]?.toFloatOrNull()
            val z = singleFields["_chem_comp_atom.model_Cartn_z"]?.toFloatOrNull()
            val id = singleFields["_chem_comp_atom.atom_id"] ?: "UNK"
            
            if (element != null && x != null && y != null && z != null) {
                atoms.add(Atom(0, element, x, y, z, id, ""))
            }
        }
        
        return Pair(atoms, bonds)
    }

    private fun splitCifLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
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
                        current = StringBuilder()
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
