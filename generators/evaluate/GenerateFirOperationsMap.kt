/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.evaluate

import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.Printer
import java.io.File

val FIR_DEST_FILE: File = File("compiler/fir/evaluate/src/org/jetbrains/kotlin/fir/evaluate/FirOperationsMapGenerated.kt")
private val EXCLUDED_FUNCTIONS: List<String> = listOf(
    "not", "toChar", "toString",
    "and", "or", "xor", "shl", "shr", "ushr",
    "compareTo", "equals",
    "rangeTo", "subSequence")
private val EXCLUDED_TYPES: List<String> = listOf("Boolean", "Char", "String")

fun main() {
    GeneratorsFileUtil.writeFileIfContentChanged(FIR_DEST_FILE, generateFirMap())
}

fun generateFirMap(): String {
    val sb = StringBuilder()
    val p = Printer(sb)
    p.println(File("license/COPYRIGHT.txt").readText())

    p.println("""
        |package org.jetbrains.kotlin.fir.evaluate
        |
        |import org.jetbrains.kotlin.fir.expressions.FirConstKind
        |
        |/** This file is generated by org.jetbrains.kotlin.generators.evaluate:generateFirMap(). DO NOT MODIFY MANUALLY */
        |
        |internal data class UnaryOperationKey<out T>(val opr: FirConstKind<out T>, val opName: String)
        |internal data class BinaryOperationKey<out T, out U>(val opr1: FirConstKind<out T>, val opr2: FirConstKind<out U>, val opName: String)
        |
        |@Suppress("UNCHECKED_CAST")
        |private fun <T> unaryOperation(
        |    t: FirConstKind<T>,
        |    opName: String,
        |    operation: Function1<T, Number>
        |) = UnaryOperationKey(t, opName) to operation as Function1<Number, Number>
        |
        |@Suppress("UNCHECKED_CAST")
        |private fun <T, U> binaryOperation(
        |    t: FirConstKind<T>,
        |    u: FirConstKind<U>,
        |    opName: String,
        |    operation: Function2<T, U, Number>
        |) = BinaryOperationKey(t, u, opName) to operation as Function2<Number, Number, Number>
    """.trimMargin())
    p.println()

    val unaryOperationsMap = arrayListOf<Triple<String, List<KotlinType>, Boolean>>()
    val binaryOperationsMap = arrayListOf<Pair<String, List<KotlinType>>>()
    populateOperationsMap(unaryOperationsMap, binaryOperationsMap, EXCLUDED_FUNCTIONS)

    p.println("internal val unaryOperations: HashMap<UnaryOperationKey<*>, Function1<Number, Number>> = hashMapOf(")
    p.pushIndent()
    p.renderUnaryOperations(unaryOperationsMap)
    p.popIndent()
    p.println(")")
    p.println()

    p.println("internal val binaryOperations: HashMap<BinaryOperationKey<*, *>, Function2<Number, Number, Number>> = hashMapOf(")
    p.pushIndent()
    p.renderBinaryOperations(binaryOperationsMap)
    p.popIndent()
    p.println(")")

    return sb.toString()
}

private fun Printer.renderUnaryOperations(unaryOperationsMap: List<Triple<String, List<KotlinType>, Boolean>>) {
    val unaryOperationsMapIterator = unaryOperationsMap.iterator()
    while (unaryOperationsMapIterator.hasNext()) {
        val (funcName, parameters, isFunction) = unaryOperationsMapIterator.next()
        if (parameters.first().constructor.declarationDescriptor!!.name.asString() in EXCLUDED_TYPES) {
            continue
        }
        val parenthesesOrBlank = if (isFunction) "()" else ""
        this.println(
            "unaryOperation(",
            parameters.joinToString(", ") { it.asString() },
            ", ",
            "\"$funcName\"",
            ", { a -> a.$funcName$parenthesesOrBlank })",
            if (unaryOperationsMapIterator.hasNext()) "," else ""
        )
    }
}

private fun Printer.renderBinaryOperations(binaryOperationsMap: List<Pair<String, List<KotlinType>>>) {
    val binaryOperationsMapIterator = binaryOperationsMap.iterator()
    while (binaryOperationsMapIterator.hasNext()) {
        val (funcName, parameters) = binaryOperationsMapIterator.next()
        if (parameters.first().constructor.declarationDescriptor!!.name.asString() in EXCLUDED_TYPES) {
            continue
        }
        this.println(
            "binaryOperation(",
            parameters.joinToString(", ") { it.asString() },
            ", ",
            "\"$funcName\"",
            ", { a, b -> a.$funcName(b) })",
            if (binaryOperationsMapIterator.hasNext()) "," else ""
        )
    }
}

private fun KotlinType.asString(): String = "FirConstKind." + constructor.declarationDescriptor!!.name.asString()
