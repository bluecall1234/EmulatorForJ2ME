# Task: J2ME Emulator Development & Debugging

## Phase 1: Research & Context Gathering
- [x] Read previous conversation artifacts (`task.md`, `walkthrough.md`)
- [x] Review current implementation in key files (`JarLoader.kt`, `NativeGraphicsBridge.kt`, `ExecutionEngine.kt`, `NativeMethodBridge.kt`)
- [x] Identify any remaining "Phase 8" tasks or new crash reports

## Phase 2: Input & Performance (Current)
- [ ] Connect Virtual Keyboard events
    - [x] Implement java.lang.String methods (trim, length, startsWith, etc.)
    - [x] Implement java.lang.StringBuffer methods (append, toString, etc.)
    - [x] Implement java.util.Vector and java.util.Hashtable
    - [x] Implement java.lang.Integer and Canvas.getWidth/Height
    - [x] Implement Vector search methods and java.lang.Boolean
    - [x] Implement missing `dup2` and arithmetic opcodes in `ExecutionEngine.kt`
    - [x] Fix parameter pop order in `Graphics.drawSubstring`
    - [x] Define missing opcode constants in `Opcodes.kt`
    - [x] Implement missing Graphics, Font, and Display methods
    - [x] Debug why sprites are not appearing (Asset Loading)
    - [x] Implement DirectUtils (Nokia API) for transparent images / sprites
    - [x] Implement Timer/TimerTask for game loop
    - [x] Implement GameCanvas backbuffer and ByteArrayOutputStream
    - [x] Resolve White Screen issue (Analyze why game fills white)
    - [x] Implement missing J2ME APIs (DataOutputStream, readLong, drawSubstring, etc.)
    - [x] Implement InputStream.read([BII)I to resolve lookup failures
    - [x] Fix stuck game after SplashScreen (Implement InputStream.read([B)I and BAOS.write([BII)V)
    - [x] Support off-screen text drawing (fix missing menu text)
    - [x] Implement J2ME anchor handling in drawImage and drawRegion
    - [/] Trigger bytecode paint() from repaint() to update game screen
    - [ ] Investigate JNI layer rendering and remove log gating
    - [ ] Add more diagnostics to Image.createImage if sprites still missing
- [ ] Implement FastArrayHeapObject for Performance Memory Mode
- [ ] Investigate any potential new crashes during game sessions
