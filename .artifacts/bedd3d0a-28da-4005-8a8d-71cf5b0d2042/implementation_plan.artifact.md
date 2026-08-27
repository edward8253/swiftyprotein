# Implementation Plan: 3D Molecule Viewer (Ligand)

Replace the placeholder `ConfirmationScreen` with a high-performance 3D viewer for ligands using the Google Filament engine.

## User Review Required

> [!IMPORTANT]
> - **Rendering Engine**: I have chosen **Google Filament** for its high performance (60 FPS target), physically based rendering (PBR), and robust Android support.
> - **Data Format**: I will switch from `.cif` to `.pdb` for downloading ligand data, as PDB is easier to parse for coordinates and connectivity (`CONECT` records) which are essential for the Ball-and-Stick model.
> - **Interaction**: The implementation will include a custom gesture detector for smooth rotation, zoom, and panning within the 3D scene.

## Proposed Changes

### Dependencies

#### [MODIFY] [libs.versions.toml](file:///C:/Personal/Projects/SwiftyProtein/gradle/libs.versions.toml)
- Add `filament-android` version and library definition.

#### [MODIFY] [app/build.gradle.kts](file:///C:/Personal/Projects/SwiftyProtein/app/build.gradle.kts)
- Add Filament dependencies.

### Data Layer

#### [NEW] [PdbModels.kt](file:///C:/Personal/Projects/SwiftyProtein/app/src/main/java/com/swiftyprotein/data/PdbModels.kt)
- Define `Atom` and `Bond` data classes.
- Define `CpkColors` mapping.

#### [NEW] [PdbParser.kt](file:///C:/Personal/Projects/SwiftyProtein/app/src/main/java/com/swiftyprotein/data/PdbParser.kt)
- Logic to parse PDB file content into a list of atoms and bonds.

#### [MODIFY] [LigandListScreen.kt](file:///C:/Personal/Projects/SwiftyProtein/app/src/main/java/com/swiftyprotein/ui/screens/LigandListScreen.kt)
- Update `downloadCif` to fetch `.pdb` files from RCSB.
- Update navigation to the new detail screen.

### 3D Rendering (Filament)

#### [NEW] [FilamentHelper.kt](file:///C:/Personal/Projects/SwiftyProtein/app/src/main/java/com/swiftyprotein/ui/filament/FilamentHelper.kt)
- Helper class to initialize the Filament engine, scene, and camera.

#### [NEW] [MoleculeRenderer.kt](file:///C:/Personal/Projects/SwiftyProtein/app/src/main/java/com/swiftyprotein/ui/filament/MoleculeRenderer.kt)
- Procedural geometry generation for spheres (atoms) and cylinders (bonds).
- Apply CPK colors and lighting.

### UI Screens

#### [NEW] [LigandDetailScreen.kt](file:///C:/Personal/Projects/SwiftyProtein/app/src/main/java/com/swiftyprotein/ui/screens/LigandDetailScreen.kt) [REPLACES ConfirmationScreen]
- `AndroidView` for Filament's `SurfaceView`.
- Gesture handling (Rotation, Pinch-to-Zoom, Two-finger Pan).
- Atom picking (on-tap detection) and info tooltip.
- Share button implementation (Screenshot + Share Intent).

#### [DELETE] [ConfirmationScreen.kt](file:///C:/Personal/Projects/SwiftyProtein/app/src/main/java/com/swiftyprotein/ui/screens/ConfirmationScreen.kt)
- Remove the placeholder screen.

#### [MODIFY] [MainActivity.kt](file:///C:/Personal/Projects/SwiftyProtein/app/src/main/java/com/swiftyprotein/MainActivity.kt)
- Update navigation routes to use `LigandDetailScreen`.

## Verification Plan

### Automated Tests
- Unit tests for `PdbParser` with sample PDB data.

### Manual Verification
- Deploy to an Android device/emulator.
- Select various ligands (e.g., HEM, ATP, GLC) from the list.
- Verify 3D rendering:
    - [ ] Correct colors (CPK).
    - [ ] Ball-and-Stick structure.
    - [ ] Smooth rotation/zoom/pan.
    - [ ] Atom info popup on tap.
- Verify Share functionality:
    - [ ] Screenshot captures the 3D model.
    - [ ] Native share sheet opens correctly.
- Verify Performance:
    - [ ] Monitor frame rate during interactions (target 60 FPS).
