sealed class Result<out T>()

data class Success<T> (
    val result : T,
    val unconsumed : String
) : Result<T>()

object Fail : Result<Nothing>()

fun <T,S> fmapResult(f : (T) -> S, arg : Result<T>) : Result<S> =
    when(arg) {
        is Fail -> Fail
        is Success -> Success(f(arg.result), arg.unconsumed)

}


interface Parser<T> {
    fun parse(input : String) : Result<T>
}

object EmptyParser : Parser<Unit> {
    override fun parse(input : String) : Result<Unit> = Success(Unit, input)
}

class ReturnParser<T> (
    val arg : T
) : Parser<T> {
    override fun parse(input: String): Result<T> = Success(arg, input)
}

class FailParser<T> : Parser<T> {
    override fun parse(input : String): Result<T> = Fail
}

class CharParser (
    val char : Char
) : Parser<Char> {
    override fun parse(input: String): Result<Char> {
        return if (input.isEmpty() || input.first() != char)
            Fail
        else
            Success(char, input.drop(1))
    }
}

class Alt<T> (
    val left : Parser<T>,
    var right : Parser<T>
) : Parser<T> {
    override fun parse(input: String) : Result<T> =
        when (val leftResult : Result<T> = left.parse(input)) {
            is Fail -> right.parse(input)
            is Success -> leftResult
        }
}

class Seq<T, S> (
    val first : Parser<T>,
    var second : (T) -> Parser<S>
) : Parser<S> {
    override fun parse(input: String) : Result<S> =
        when(val firstResult : Result<T> = first.parse(input)) {
            is Fail -> Fail
            is Success ->
                second(firstResult.result).parse(firstResult.unconsumed)
        }
}

class Or<T> (
    val parsers : ArrayList<Parser<T>>
) : Parser<T> {
    override fun parse(input: String) : Result<T> {
        val parser =
            parsers.fold(
                FailParser(),
                {acc: Parser<T>, x: Parser<T> -> Alt(acc, x)})
        return parser.parse(input)
    }
}

class StarParser<T> (
    val parser : Parser<T>
) : Parser<ArrayList<T>> {
    override fun parse(input: String): Result<ArrayList<T>> {
        val emptyParser : Parser<ArrayList<T>> = ReturnParser(arrayListOf())
        val starParser : Parser<ArrayList<T>> =
            Seq(parser, { res: T -> Map({lst : ArrayList<T> -> lst.add(0, res); lst}, StarParser(parser))})
        val or : Parser<ArrayList<T>> = Alt(starParser, emptyParser)
        return or.parse(input)
    }
}

class Map <T, S> (
    val f : (T) -> S,
    val parser : Parser<T>
) : Parser<S> {
    override fun parse(input: String): Result<S> = 
        when(val parsingResult : Result<T> = parser.parse(input)) {
            is Fail -> Fail 
            is Success -> Success(f(parsingResult.result), parsingResult.unconsumed)
        }
}

class Sequence<T> (
    val parsers : ArrayList<Parser<T>>
) : Parser<ArrayList<T>> {
    override fun parse(input: String): Result<ArrayList<T>> {
        val initParser : Parser<ArrayList<T>> = ReturnParser(arrayListOf<T>())
        val parser : Parser<ArrayList<T>> =
            parsers.fold(
                initParser,
                {acc : Parser<ArrayList<T>>,
                 x : Parser<T> ->
                     Seq(acc,
                         {lst : ArrayList<T> ->
                             Map({t : T -> lst.add(0, t)
                                           lst
                             }, x)})
                }
            )
        return parser.parse(input)
    }
}

fun word(str: String) : Parser<String> {
    val charParserList : ArrayList<Parser<Char>> = str.map({ char: Char -> CharParser(char) }).toCollection(ArrayList())
    return Map({lst: ArrayList<Char> -> lst.foldRight("", {x : Char, acc : String -> acc + x})},
               Sequence(charParserList))
}

fun number() : Parser<Int> {
    fun digitToInt(c : Char) : Int = c.toInt() - '0'.toInt()
    fun makeNumber(lst : ArrayList<Char>) : Int =
        if(lst.isEmpty())
            0
        else
            digitToInt(lst.first()) + 10 * makeNumber(lst.drop(1).toCollection(ArrayList()))

    val nonZeroDigit : Parser<Char> = anySymbol("123456789")
    val digit : Parser<Char> = Alt(CharParser('0'),  nonZeroDigit)

    val numParser : Parser<ArrayList<Char>> =
        Seq(nonZeroDigit,
            {d : Char -> Map({lst : ArrayList<Char> -> lst.add(0, d); lst} , StarParser(digit))})

    return Map({ lst : ArrayList<Char> -> makeNumber(lst.reversed().toCollection(ArrayList()))}, numParser)
}

val lbr : Parser<Char>  = CharParser('(')
val rbr : Parser<Char>  = CharParser(')')
val plus : Parser<Char> = CharParser('+')
val mult : Parser<Char> = CharParser('*')

fun <T,S> nonEmptyListParser(separator : Parser<S>, elemParser : Parser<T>) : Parser<ArrayList<T>> {
    val sepThenElem : Parser<T> = Seq(separator, { elemParser })

    return Seq(elemParser,
              {elem : T -> Map({lst : ArrayList<T> -> lst.add(0, elem); lst},
                                StarParser(sepThenElem))})
}

fun <T,S> inBrackets(lbr : Parser<S>, rbr : Parser<S>, parser : Parser<T>) : Parser<T> =
    Seq(lbr, {Seq(parser, {res -> Map({res}, rbr)})})

object myFavParser : Parser<AST<Int>> {
    val exprInBrackets : Parser<AST<Int>> = inBrackets(lbr, rbr, this)

    val numParser : Parser<AST<Int>> = Map({n: Int -> Number(n)}, number())

    val primaryParser : Parser<AST<Int>> =
        Alt(exprInBrackets, numParser)

    val mulParser : Parser<AST<Int>> = Map({multipliers : ArrayList<AST<Int>> -> Multiplication(multipliers)},
        nonEmptyListParser(mult, primaryParser))

    val sumParser : Parser<AST<Int>> = Map({addendums : ArrayList<AST<Int>> -> Addition(addendums)},
        nonEmptyListParser(plus, mulParser))

    override fun parse(input: String): Result<AST<Int>> =
        sumParser.parse(input)
}

fun anySymbol(str: String) : Parser<Char> =
    Or(str.map({ char: Char -> CharParser(char) }).toCollection(ArrayList()))

