# Implementation Plan - Graphics API Completion

This plan outlines the addition of missing MIDP and common extension methods to the `javax.microedition.lcdui.Graphics` class to improve compatibility with games like "Bounce Tales".

## Proposed Changes

### [MIDP-Nokia Graphics]
The following methods will be added as `abstract` to the `Graphics` class. These are either part of newer MIDP specifications or are widely used extensions implemented in the emulator's backend.

#### [MODIFY] [Graphics.java](file:///D:/WebTrainning/Antigravity/EmulatorForJ2ME/src_MIDP/src/javax/microedition/lcdui/Graphics.java)
- Add `abstract` methods for:
    - `getRGB(int[] argbData, int offset, int scanlength, int x, int y, int width, int height)`
    - `setRGB(int x, int y, int w, int h, int[] argbData, int offset, int scanlength)`
    - `drawPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints)`
    - `fillPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints)`
    - `drawImagePart(Image image, int x, int y, int width, int height)`
    - `setARGBColor(int argb)`
    - `drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor)`
    - `fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor)`
    - `getAlphaComponent()`
    - `getNativePixelFormat()`

- Update `Graphics` class to implement `com.nokia.mid.ui.DirectGraphics` interface.

## Verification Plan

### Manual Verification
- Rebuild the emulator bridge.
- Run "Bounce Tales" and check if the missing methods cause any linkage issues.
- Verify screen rendering (this should not break anything, only add stubs/abstract declarations).
