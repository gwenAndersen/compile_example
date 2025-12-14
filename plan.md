# Implementation Plan

## Phase 1: Data Ingestion (Done)

*   [x] Modify the `Live3dWallpaperService` to observe the `SharedViewModel` for changes to the captured layout JSON.
*   [x] Parse the JSON data into a `Map<Int, List<IconInfo>>`.
*   [x] Pass the parsed data to the `AlyfWlpGLRenderer`.

## Phase 2: Icon Rendering (Done)

*   [x] Modify `AlyfWlpGLRenderer` to receive the layout data.
*   [x] Create a new data class, `Icon`, to hold the `IconInfo` and the loaded `Bitmap` of the icon.
*   [x] Create a texture for each icon and store the texture ID in the `Icon` object.
*   [x] In `onDrawFrame`, iterate through the icons and draw each one as a textured quad.

## Phase 3: Positioning and Scaling (Done)

*   [x] Convert the icon bounds from screen coordinates to OpenGL coordinates.
*   [x] Create a separate vertex buffer for each icon, with the vertices corresponding to the icon's bounds.
*   [x] Use a projection matrix to correctly map the OpenGL coordinates to the screen.

## Phase 4: Text Rendering (Optional)

*   [ ] Render the app names (from `IconInfo.text`) below each icon. This is a complex task in OpenGL and may require a separate library or a custom implementation.

## Phase 5: 3D Effects and Animations (Optional)

*   [ ] Add 3D effects, such as a parallax effect when the device is tilted.
*   [ ] Add animations to the icons, such as a "jiggle" mode or animations when an icon is "launched" (which would require more interaction with the accessibility service).
