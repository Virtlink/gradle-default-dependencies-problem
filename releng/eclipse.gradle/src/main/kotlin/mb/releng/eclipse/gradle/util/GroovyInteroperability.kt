package mb.releng.eclipse.gradle.util

import groovy.lang.Closure

// It seems to be impossible to use the `kotlin-dsl` plugin, causing all kinds of compiler errors. Importing functions
// from the Kotlin DSL project directly also seems impossible. For now, we copy them.

/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//
// From: https://github.com/gradle/kotlin-dsl/blob/75ad0e760f9c6fe2d3f0728f60837a0b22184566/subprojects/provider/src/main/kotlin/org/gradle/kotlin/dsl/GroovyInteroperability.kt
//

/**
 * Adapts a Kotlin function to a single argument Groovy [Closure].
 *
 * @param T the expected type of the single argument to the closure.
 * @param action the function to be adapted.
 *
 * @see [KotlinClosure1]
 */
fun <T> Any.closureOf(action: T.() -> Unit): Closure<Any?> = KotlinClosure1(action, this, this)

/**
 * Adapts a Kotlin function to a Groovy [Closure] that operates on the
 * configured Closure delegate.
 *
 * @param T the expected type of the delegate argument to the closure.
 * @param action the function to be adapted.
 *
 * @see [KotlinClosure1]
 */
fun <T> Any.delegateClosureOf(action: T.() -> Unit) = object : Closure<Unit>(this, this) {
  @Suppress("unused") // to be called dynamically by Groovy
  fun doCall() = uncheckedCast<T>(delegate).action()
}

/**
 * Adapts an unary Kotlin function to an unary Groovy [Closure].
 *
 * @param T the type of the single argument to the closure.
 * @param V the return type.
 * @param function the function to be adapted.
 * @param owner optional owner of the Closure.
 * @param thisObject optional _this Object_ of the Closure.
 *
 * @see [Closure]
 */
class KotlinClosure1<in T : Any?, V : Any>(
  val function: T.() -> V?,
  owner: Any? = null,
  thisObject: Any? = null
) : Closure<V?>(owner, thisObject) {

  @Suppress("unused") // to be called dynamically by Groovy
  fun doCall(it: T): V? = it.function()
}

//
// From https://github.com/gradle/kotlin-dsl/blob/75ad0e760f9c6fe2d3f0728f60837a0b22184566/subprojects/provider/src/main/kotlin/org/gradle/kotlin/dsl/support/Cast.kt
//

@Suppress("unchecked_cast", "nothing_to_inline")
internal
inline fun <T> uncheckedCast(obj: Any?): T = obj as T
