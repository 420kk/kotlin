// Code generation problem with JVM backend.
// IGNORE_BACKEND: JVM
// FILE: test.kt
class A {
    suspend fun foo() {}
}

suspend fun box() {
    A().foo()
}

// EXPECTATIONS
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:4 <init>:
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:5 foo: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:10 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
