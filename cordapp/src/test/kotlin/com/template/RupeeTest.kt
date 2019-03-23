package com.template

fun main(args: Array<String>){


    val mylist = listOf<Int>(1,3,5,7)
    println(mylist.contains(5))
    println(mylist.contains(9))

    val olist = listOf<Int>(1,10,5)

    println(mylist.containsAll(olist))

    val rupee1  = Rupee("BG", 100)
    val ruppe2 = Rupee("BG", 20)
    val unit = 110
    val newOwner = "Sagar"

    val rupeeList = listOf<Rupee>(rupee1,ruppe2)
    val myRupee = getMyChange(rupeeList, unit, newOwner)

    println(myRupee.otherPartyRupee)
    println(myRupee.myRupee)
}

fun getMyChange(rupees: List<Rupee>, unit : Int, newOwner: String) : RupeeChange{

    val total = rupees.map { it.amount }.sum()
    val totalRupee = rupees.first().copy(amount = total)
    return RupeeChange(totalRupee.copy(newOwner,unit),totalRupee.copy(amount = total-unit))
}

data class Rupee( val owner: String, val amount: Int){

    fun withNewOwner(newOwner: String) : Rupee{
        return this.copy(owner= newOwner)
    }

    fun withoutOwner() : Rupee{
        return this.copy(owner= "")
    }
}

data class RupeeChange(val otherPartyRupee: Rupee, val myRupee: Rupee)

