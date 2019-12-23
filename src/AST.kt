sealed class AST<out T>()

data class Addition<T>(
    val addendums: List<AST<T>>
) : AST<T>()

data class Multiplication<T>(
    val multipliers: List<AST<T>>
) : AST<T>()

data class Number<T>(
    val number: T
) : AST<T>()

fun <T> nicelyPrint(tree: AST<T>): String =
    when (tree) {
        is Number -> tree.number.toString()
        is Multiplication ->
            tree.multipliers.joinToString(
                separator = "*",
                prefix = "(",
                postfix = ")",
                transform = { x: AST<T> -> nicelyPrint(x) })
        is Addition ->
            tree.addendums.joinToString(
                separator = "+",
                prefix = "(",
                postfix = ")",
                transform = { x: AST<T> -> nicelyPrint(x) })
    }

fun evaluateExpr(tree: AST<Int>): Int =
    when (tree) {
        is Number -> tree.number
        is Addition ->
            tree.addendums.fold(
                0,
                { acc: Int, x: AST<Int> -> acc + evaluateExpr(x) })
        is Multiplication ->
            tree.multipliers.fold(
                1,
                { acc: Int, x: AST<Int> -> acc * evaluateExpr(x) })
    }