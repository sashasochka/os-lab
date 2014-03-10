package object dfa {
  type Matches = List[String]
  type Action = ((Char, Matches) => Matches)
  type Transition = Map[Symbol, (EmailDFAState, Action)]

  class Symbol
  val AtSymbol
    , DashSymbol
    , DotSymbol
    , UnderlineSymbol
    , WhitespaceSymbol
    , LetterOrDigitSymbol
    , OtherSymbol
      = new Symbol


  val StartGroup: Action = (char, matches) =>
    char.toString :: matches

  val AddToLastGroup: Action = (char, matches) =>
    (matches.head + char) :: matches.tail

  val NoAction: Action = (char, matches) =>
    matches
}
