package com.laputa.seekbar

open class Base<out T>
class Derive<T>(var value: T) : Base<T>()

fun unsound() {
    val d = Derive(0)
    val b: Base<Any> = d
    if ((b as Derive<Int>) is Derive<*>) {
        b.value = 222
    }
    println(d.value + 2)

}

fun unsound2() {
    val d = Derive(0)
    val b: Base<Any> = d
    if (b is Derive) {
        b.value = "222"
    }
    println(d.value + 2)

}

fun a() {
    a()
}

class Demo {
    lateinit var  b:()->Unit

}
