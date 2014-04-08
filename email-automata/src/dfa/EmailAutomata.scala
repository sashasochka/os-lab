package dfa

object EmailDFA {
  val startState: EmailDFAState = BeginState
  val endStates: List[EmailDFAState] = List(BeginState, MidOrEndOfSubdomainState)

  final def matches(string: String): Boolean =
    matchStrings(string).isDefined

  final def nonMatches(string: String): Boolean =
    !matches(string)

  final def matchStrings(string: String): Option[Matches] = {
    var state = startState
    var matches: Matches = Nil
    for (c <- string) {
      state nextStateOption c match {
        case Some((newState, action)) =>
          state = newState
          matches = action(c, matches)
        case None => return None
      }
    }
    if (endStates contains state) Some(matches.reverse)
    else None
  }
}

abstract sealed class EmailDFAState {
  implicit val charToSymbol: (Char => Symbol) = {
    case c if c.isWhitespace => WhitespaceSymbol
    case c if c.isLetterOrDigit => LetterOrDigitSymbol
    case '@' => AtSymbol
    case '-' => DashSymbol
    case '.' => DotSymbol
    case '_' => UnderlineSymbol
    case _ => OtherSymbol
  }

  final def nextStateOption(c: Char) =
    transition lift c

  val transition: Transition
}

object BeginState extends EmailDFAState {
  override val transition: Transition = Map(
    WhitespaceSymbol -> (BeginState, NoAction),
    LetterOrDigitSymbol -> (MidOrEndOfNameState, StartGroup),
    UnderlineSymbol -> (MidOrEndOfNameState, StartGroup)
  )
}

object MidOrEndOfNameState extends EmailDFAState {
  override val transition: Transition = Map(
    LetterOrDigitSymbol -> (MidOrEndOfNameState, AddToLastGroup),
    UnderlineSymbol -> (MidOrEndOfNameState, AddToLastGroup),
    AtSymbol -> (BeginOfDomainState, NoAction)
  )
}

object BeginOfDomainState extends EmailDFAState {
  override val transition: Transition = Map(
    LetterOrDigitSymbol -> (MidOrEndOfSubdomainState, StartGroup)
  )
}

object BeginOfSubdomainState extends EmailDFAState {
  override val transition: Transition = Map(
    LetterOrDigitSymbol -> (MidOrEndOfSubdomainState, AddToLastGroup)
  )
}

object MidOfSubdomainState extends EmailDFAState {
  override val transition: Transition = Map(
    DashSymbol -> (MidOfSubdomainState, AddToLastGroup),
    LetterOrDigitSymbol -> (MidOrEndOfSubdomainState, AddToLastGroup)
  )
}

object MidOrEndOfSubdomainState extends EmailDFAState {
  override val transition: Transition = Map(
    DashSymbol -> (MidOfSubdomainState, AddToLastGroup),
    DotSymbol -> (BeginOfSubdomainState, AddToLastGroup),
    WhitespaceSymbol -> (BeginState, NoAction),
    LetterOrDigitSymbol -> (MidOrEndOfSubdomainState, AddToLastGroup)
  )
}

object Main extends App {
  val text = io.Source.stdin.mkString
  val output = EmailDFA.matchStrings(text)
    .map(_.mkString("Your string matches! Captured strings: \n", "\n", ""))
    .getOrElse("Doesn't match")
  println(output)
}
