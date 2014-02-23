import org.scalatest.FlatSpec
import EmailDFA._

class EmailDFASpec extends FlatSpec {
  behavior of "toEmailDFASymbols"
  it should "map empty string to Nil" in {
    assert(EmailDFA.toDFASymbols("") == Nil)
  }

  it should "map longer string to DFS symbols correctly" in {
    assert(EmailDFA.toDFASymbols("azAZ09-_. \n@&(") == List(
      LetterOrDigitSymbol, LetterOrDigitSymbol,
      LetterOrDigitSymbol, LetterOrDigitSymbol,
      LetterOrDigitSymbol, LetterOrDigitSymbol,
      DashSymbol, UnderlineSymbol, DotSymbol,
      WhitespaceSymbol, WhitespaceSymbol,
      AtSymbol, OtherSymbol, OtherSymbol
    ))
  }

  behavior of "EmailDFA"
  it should "match empty string" in {
    assert(EmailDFA.matches(""))
  }

  it should "match simple email" in {
    assert(EmailDFA.matches("hello@gmail.com"))
  }

  it should "match email with only tld" in {
    assert(EmailDFA.matches("hello@com"))
  }

  it should "match email with trailing spaces" in {
    assert(EmailDFA.matches("  hello@com\n "))
  }

  it should "match email with different usernames" in {
    assert(EmailDFA.matches("abc3@gmail.com"))
    assert(EmailDFA.matches("2abc@gmail.com"))
    assert(EmailDFA.matches("a4bc@gmail.com"))
    assert(EmailDFA.matches("Ab3c@gmail.com"))
    assert(EmailDFA.matches("Ab3C@gmail.com"))
    assert(EmailDFA.matches("Ab3Z@gmail.com"))
    assert(EmailDFA.matches("Ab3_@gmail.com"))
    assert(EmailDFA.matches("b3_@gmail.com"))
    assert(EmailDFA.matches("_@gmail.com"))
  }

  it should "match email with multiple level domains" in {
    assert(EmailDFA.matches("abc@gmail.omg.fds.mfd.com"))
  }

  it should "match multiple emails" in {
    assert(EmailDFA.matches(" a@gmail.com adf3f@meta.ua "))
    assert(EmailDFA.matches("a@gmail.com adf3f@meta.ua"))
  }

  it should "match complex set of emails" in {
    assert(EmailDFA.matches(" a3_omg2@gmailfd.sfd.jk.com \nadfdf_3f@m-e-t-a.u-a " +
      " 3asd_@a Q4@Z3--4.434.234 myemail@192.168.1.100\t sashasochka@gmail.com "))
  }

  it should "not match email with subdomain starting/ending with dash" in {
    assert(!EmailDFA.matches("hello@omg-.com"))
    assert(!EmailDFA.matches("hello@omg.-com"))
  }

  it should "not match email without domain" in {
    assert(!EmailDFA.matches("hello@"))
  }

  it should "not match email without username" in {
    assert(!EmailDFA.matches("@omg.omg"))
  }

  it should "not match email ending with dot" in {
    assert(!EmailDFA.matches("hello@omg."))
  }
}
