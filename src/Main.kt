// 13 + 42 * 69
val tree = Addition(
    listOf(
        Number(13),
        Multiplication(
            listOf(
                Number(42),
                Number(69)))))


fun main(args: Array<String>) {
    println(nicelyPrint(tree))
    println(evaluateExpr(tree))

    println(word("abc").parse("abcd"))
    println(number().parse("123"))

    println(nonEmptyListParser(plus, number()).parse("123+45+678"))

    println(myFavParser.parse("123+45*6+78"))
    println(fmapResult({arg -> nicelyPrint(arg)}, myFavParser.parse("123+45*6+78")))
    println(fmapResult({arg -> evaluateExpr(arg)}, myFavParser.parse("123+45*6+78")))

    println(myFavParser.parse("(123+45)*6+78"))
    println(fmapResult({arg -> nicelyPrint(arg)}, myFavParser.parse("(123+45)*6+78")))
    println(fmapResult({arg -> evaluateExpr(arg)}, myFavParser.parse("(123+45)*6+78")))
}