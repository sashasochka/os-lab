import scala.annotation.tailrec

trait DFA[State, Symbol] {
  val startState: State
  val endStates: List[State]

  def transition(state: State, symbol: Symbol): Option[State]

  @tailrec
  final def matches(symbols: List[Symbol], state: State = startState): Boolean = symbols match {
    case symbol :: other => transition(state, symbol) match {
      case Some(newState) => matches(other, newState)
      case None => false
    }
    case Nil => endStates contains state
  }
}

object EmailDFA extends DFA[EmailDFAState, EmailDFASymbol] {
  val startState = BeginState
  val endStates = List(BeginState, MidOrEndOfSubdomainState)
  def transition(state: EmailDFAState, symbol: EmailDFASymbol) = (state, symbol) match {
    case (BeginState, WhitespaceSymbol) => Some(BeginState)
    case (BeginState, LetterOrDigitSymbol) => Some(MidOrEndOfNameState)
    case (BeginState, UnderlineSymbol) => Some(MidOrEndOfNameState)
    case (MidOrEndOfNameState, LetterOrDigitSymbol) => Some(MidOrEndOfNameState)
    case (MidOrEndOfNameState, UnderlineSymbol) => Some(MidOrEndOfNameState)
    case (MidOrEndOfNameState, AtSymbol) => Some(BeginOfSubdomainState)
    case (BeginOfSubdomainState, LetterOrDigitSymbol) => Some(MidOrEndOfSubdomainState)
    case (MidOfSubdomainState, DashSymbol) => Some(MidOfSubdomainState)
    case (MidOfSubdomainState, LetterOrDigitSymbol) => Some(MidOrEndOfSubdomainState)
    case (MidOrEndOfSubdomainState, DashSymbol) => Some(MidOfSubdomainState)
    case (MidOrEndOfSubdomainState, DotSymbol) => Some(BeginOfSubdomainState)
    case (MidOrEndOfSubdomainState, WhitespaceSymbol) => Some(BeginState)
    case (MidOrEndOfSubdomainState, LetterOrDigitSymbol) => Some(MidOrEndOfSubdomainState)
    case _ => None
  }

  implicit def toDFASymbols(str: String) = str.map({
    case c if c.isWhitespace => WhitespaceSymbol
    case c if c.isLetterOrDigit => LetterOrDigitSymbol
    case '@' => AtSymbol
    case '-' => DashSymbol
    case '.' => DotSymbol
    case '_' => UnderlineSymbol
    case _ => OtherSymbol
  }).toList
}

class EmailDFAState
object BeginState extends EmailDFAState
object MidOrEndOfNameState extends EmailDFAState
object BeginOfSubdomainState extends EmailDFAState
object MidOfSubdomainState extends EmailDFAState
object MidOrEndOfSubdomainState extends EmailDFAState

class EmailDFASymbol
object WhitespaceSymbol extends EmailDFASymbol
object LetterOrDigitSymbol extends EmailDFASymbol
object AtSymbol extends EmailDFASymbol
object DashSymbol extends EmailDFASymbol
object DotSymbol extends EmailDFASymbol
object UnderlineSymbol extends EmailDFASymbol
object OtherSymbol extends EmailDFASymbol

object Main extends App {
  import EmailDFA._
  val text = io.Source.stdin.mkString
  if (EmailDFA.matches(text)) println("Matches")
  else println("Doesn't match")
}
