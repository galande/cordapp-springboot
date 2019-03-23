package com.template

fun main(args: Array<String>){
    val car1 = Car("Honda","BG", 5000)

    val car2 = car1.withNewOwner("Sagar")

    val carList = listOf<Car>(car1,car2)
    println(carList)

    val result = carList.map {it.price }.sum()
    val car3 = car1.copy(price = carList.map {it.price }.sum())
    println(car3)
}

data class Car(val model: String, val owner: String, val price: Int){

    fun withNewOwner(newOwner: String) : Car{
        return this.copy(owner= newOwner)
    }

    fun withoutOwner() : Car{
        return this.copy(owner= "")
    }

//    fun reduceStates (cars: List<Car>) : Car{
//
//        val total = 0
//        for (car in cars){
//
//        }
//
//    }
}