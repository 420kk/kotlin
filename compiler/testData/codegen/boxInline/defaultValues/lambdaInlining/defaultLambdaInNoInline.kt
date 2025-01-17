// SKIP_INLINE_CHECK_IN: inlineFun$default
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

inline fun inlineFun(crossinline inlineLambda: () -> String = { "OK" }, noinline noInlineLambda: () -> String = { inlineLambda() }): String {
    return noInlineLambda()
}

// FILE: 2.kt
// CHECK_CALLED_IN_SCOPE: function=inlineFun$lambda_0 scope=box TARGET_BACKENDS=JS
// CHECK_CALLED_IN_SCOPE: function=box$lambda scope=box IGNORED_BACKENDS=JS
import test.*

fun box(): String {
    return inlineFun()
}
