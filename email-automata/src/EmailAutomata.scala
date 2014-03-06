object EmailDFA {
  val startState: EmailDFAState = BeginState
  val endStates: List[EmailDFAState] = List(BeginState, MidOrEndOfSubdomainState)

  final def matches(string: String) =
    matchStrings(string).isDefined

  final def matchStrings(string: String): Option[List[String]] = {
    var state = startState
    var strMatches: List[String] = Nil
    for (c <- string) {
      state.nextStateData(c, strMatches) match {
        case Some((newState, matchLst)) =>
          state = newState
          strMatches = matchLst
        case None => return None
      }
    }
    if (endStates contains state) Some(strMatches.reverse)
    else None
  }
}

abstract sealed class EmailDFAState {
  implicit def charToSymbol(char: Char) = char match {
    case c if c.isWhitespace => WhitespaceSymbol
    case c if c.isLetterOrDigit => LetterOrDigitSymbol
    case '@' => AtSymbol
    case '-' => DashSymbol
    case '.' => DotSymbol
    case '_' => UnderlineSymbol
    case c => OtherSymbol
  }
  final def startGroup(char: Char, matches: List[String]) = char.toString :: matches
  final def addToLastGroup(char: Char, matches: List[String]) = (matches.head + char) :: matches.tail
  def nextStateData(char: Char, matches: List[String]): Option[(EmailDFAState, List[String])]
}

case object BeginState extends EmailDFAState {
  override def nextStateData(char: Char, matches: List[String]) = charToSymbol(char) match {
    case WhitespaceSymbol => Some(BeginState, matches)
    case LetterOrDigitSymbol => Some(MidOrEndOfNameState, startGroup(char, matches))
    case UnderlineSymbol => Some(MidOrEndOfNameState, startGroup(char, matches))
    case _ => None
  }
}

case object MidOrEndOfNameState extends EmailDFAState {
  override def nextStateData(char: Char, matches: List[String]) = charToSymbol(char) match {
    case LetterOrDigitSymbol => Some(MidOrEndOfNameState, addToLastGroup(char, matches))
    case UnderlineSymbol => Some(MidOrEndOfNameState, addToLastGroup(char, matches))
    case AtSymbol => Some(BeginOfDomainState, matches)
    case _ => None
  }
}

case object BeginOfDomainState extends EmailDFAState {
  override def nextStateData(char: Char, matches: List[String]) = charToSymbol(char) match {
    case LetterOrDigitSymbol => Some(MidOrEndOfSubdomainState, startGroup(char, matches))
    case _ => None
  }
}

case object BeginOfSubdomainState extends EmailDFAState {
  override def nextStateData(char: Char, matches: List[String]) = charToSymbol(char) match {
    case LetterOrDigitSymbol => Some(MidOrEndOfSubdomainState, addToLastGroup(char, matches))
    case _ => None
  }
}

case object MidOfSubdomainState extends EmailDFAState {
  override def nextStateData(char: Char, matches: List[String]) = charToSymbol(char) match {
    case DashSymbol => Some(MidOfSubdomainState, addToLastGroup(char, matches))
    case LetterOrDigitSymbol => Some(MidOrEndOfSubdomainState, addToLastGroup(char, matches))
    case _ => None
  }
}

case object MidOrEndOfSubdomainState extends EmailDFAState {
  override def nextStateData(char: Char, matches: List[String]) = charToSymbol(char) match {
    case DashSymbol => Some(MidOfSubdomainState, addToLastGroup(char, matches))
    case DotSymbol => Some(BeginOfSubdomainState, addToLastGroup(char, matches))
    case WhitespaceSymbol => Some(BeginState, matches)
    case LetterOrDigitSymbol => Some(MidOrEndOfSubdomainState, addToLastGroup(char, matches))
    case _ => None
  }
}

sealed class EmailDFASymbol
case object AtSymbol extends EmailDFASymbol
case object DashSymbol extends EmailDFASymbol
case object DotSymbol extends EmailDFASymbol
case object UnderlineSymbol extends EmailDFASymbol
case object WhitespaceSymbol extends EmailDFASymbol
case object LetterOrDigitSymbol extends EmailDFASymbol
case object OtherSymbol extends EmailDFASymbol

object Main extends App {
  val text = io.Source.stdin.mkString
  EmailDFA.matchStrings(text) match {
    case Some(lst) =>
      println("Your string matches! Captured strings: ")
      lst map println
    case None => println("Doesn't match")
  }
}
