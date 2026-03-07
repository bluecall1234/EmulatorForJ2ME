# Crash Analysis: J2ME Custom Classes missing Bytecode Execution

## Root Cause Analysis
The logs show:
```
[Interpreter] === Executing GloftBBPM.startApp ===
  [VM] PC=6 new
  [VM] NEW cGame
[Interpreter] Allocating memory for object cGame
  [VM] PC=15 invokespecial
  [VM] invokespecial cGame.<init>(Ljava/lang/Object;Ljava/lang/Object;)V
  [VM] WARNING: Invoking unhandled non-native method cGame.<init>(Ljava/lang/Object;Ljava/lang/Object;)V (stub)
```

The issue is within `emulator/core/interpreter/ExecutionEngine.kt`, specifically the OPCODES for method invocations (`INVOKEVIRTUAL`, `INVOKESPECIAL`, `INVOKESTATIC`, `INVOKEINTERFACE`).

Currently, the emulator attempts to call `NativeMethodBridge.callNativeMethod()` for **every** method call. If it's a known `java.*` or `javax.*` API, the bridge handles it. If it is NOT handled, `ExecutionEngine` prints `WARNING: Invoking unhandled non-native method` and **stubs it out** (pops arguments and pushes a dummy 0). 
It completely skips actually interpreting the method `cGame.<init>` because it lacks the logic to dynamically recursively create a new execution frame and execute the method's `.class` bytecode!

## Proposed Changes

### `shared/src/commonMain/kotlin/emulator/core/interpreter/ExecutionEngine.kt`

We need to rewrite the non-native method fallback path in the `INVOKEVIRTUAL`, `INVOKESPECIAL`, `INVOKESTATIC`, and `INVOKEINTERFACE` instructions.

Instead of stubbing out the method when `!isHandled`, we must:
1. Fetch the target class from the interpreter (e.g., `cGame`).
2. If it's not loaded, tell the interpreter to load it.
3. Find the method within the class matches the name and descriptor (e.g., `<init>:(...)...`).
4. If it has a `CodeAttribute` (bytecode), extract it.
5. Create a **NEW** `ExecutionFrame` using the extracted bytecode, max locals, and max stack.
6. Pop the arguments from our CURRENT frame's stack and place them into the NEW frame's `locals` reversal order. Make sure they are placed in the correct array slots (e.g., if it's an instance method, Local 0 = `this`, Local 1..N = arguments).
7. Dispatch execution to a new `ExecutionEngine` context or recursively call the bytecode loop on the new frame.
8. Once the child recursive call returns a value, we push that return value onto our CURRENT frame's stack (if return type != Void).

### `shared/src/commonMain/kotlin/emulator/core/BytecodeInterpreter.kt`
We need to enhance `BytecodeInterpreter.executeMethod(className, methodName, args)` to be dynamically callable from within another `ExecutionEngine` frame. Or better yet, we can add a new method: `executeMethod(className: String, methodName: String, descriptor: String, args: Array<Any?>): Any?` that finds exactly the right method overload. We must pass BOTH name AND descriptor because a class may have multiple `<init>` methods with different parameter counts.

## Review Required
I will request user approval on the updated plan to fix the method execution.
