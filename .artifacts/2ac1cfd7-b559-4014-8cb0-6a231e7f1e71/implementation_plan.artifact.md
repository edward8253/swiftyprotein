# Fix 3D View: Rendering for small ligands and User Interactions

The user reported two main issues:
1. Ligands with 2 molecules (atoms) or less are not shown.
2. User interactions on the 3D view don't work.

## Proposed Changes

### [Data] CifParser

#### [MODIFY] [CifParser.kt](file:///C:/Personal/Projects/SwiftyProtein/app/src/main/java/com/swiftyprotein/data/CifParser.kt)
- Update the parser to handle CIF fields that are not part of a `loop_` block. Some small ligands or ions might be defined as single fields.
- Improve robustness of field detection.

### [UI] OpenGL Renderer

#### [MODIFY] [MoleculeGlRenderer.kt](file:///C:/Personal/Projects/SwiftyProtein/app/src/main/java/com/swiftyprotein/ui/opengl/MoleculeGlRenderer.kt)
- **Dynamic Zoom & Centering**: Update `calculateCenter` to calculate the bounding box and set an initial `zoom` level that fits the molecule appropriately.
- **Optimization**: Replace `atoms.find { ... }` in `drawBond` with direct index access for better performance.
- **State Management**: Add a `resetState()` method to clear rotations and pans when a new molecule is loaded.

### [UI] Screens

#### [MODIFY] [LigandDetailScreen.kt](file:///C:/Personal/Projects/SwiftyProtein/app/src/main/java/com/swiftyprotein/ui/screens/LigandDetailScreen.kt)
- **Interaction Fix**: Remove the overlapping `Box` that was blocking touch events. Move the `detectTapGestures` to the `AndroidView` modifier chain.
- **State Reset**: Call `renderer.resetState()` in the `LaunchedEffect` when a new ligand is loaded.
- **Gesture Synchronization**: Ensure both tap and transform gestures work on the same view.

## Verification Plan

### Automated Tests
- N/A (UI and OpenGL specific)

### Manual Verification
- Load a single-atom ligand (e.g., Cl, MG) and verify it is shown and centered.
- Load a 2-atom ligand (e.g., O2) and verify it is shown.
- Verify that rotating, panning, and zooming work.
- Verify that tapping an atom still shows the tooltip.
