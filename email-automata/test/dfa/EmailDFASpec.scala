package dfa

import org.scalatest.FlatSpec

class EmailDFASpec extends FlatSpec {
  it should "match empty string" in {
    assert(EmailDFA matches "")
  }

  it should "match simple email" in {
    assert(EmailDFA matches "a@b")
    assert(EmailDFA matches "hello@gmail.com")
    assert(EmailDFA.matchStrings("hello@gmail.com").get === List("hello", "gmail.com"))
  }


  it should "match email with only tld" in {
    assert(EmailDFA matches "hello@com")
    assert(EmailDFA.matchStrings("hello@com").get === List("hello", "com"))
  }

  it should "match email with trailing spaces" in {
    assert(EmailDFA matches "  hello@com\n ")
  }

  it should "match email with different usernames" in {
    assert(EmailDFA matches "abc3@gmail.com")
    assert(EmailDFA matches "2abc@gmail.com")
    assert(EmailDFA matches "a4bc@gmail.com")
    assert(EmailDFA matches "Ab3c@gmail.com")
    assert(EmailDFA matches "Ab3C@gmail.com")
    assert(EmailDFA matches "Ab3Z@gmail.com")
    assert(EmailDFA matches "Ab3_@gmail.com")
    assert(EmailDFA matches "b3_@gmail.com")
    assert(EmailDFA.matchStrings("b3_@gmail.com").get === List("b3_", "gmail.com"))
    assert(EmailDFA matches "_@gmail.com")
  }

  it should "match email with multiple level domains" in {
    assert(EmailDFA matches "abc@gmail.omg.fds.mfd.com")
  }

  it should "match multiple emails" in {
    assert(EmailDFA matches " a@gmail.com adf3f@meta.ua ")
    assert(EmailDFA.matchStrings(" a@gmail.com adf3f@meta.ua ").get === List("a", "gmail.com", "adf3f", "meta.ua"))
    assert(EmailDFA matches "a@gmail.com adf3f@meta.ua")
  }

  it should "match complex set of emails" in {
    val str = " a3_omg2@gmailfd.sfd.jk.com \nadfdf_3f@m-e-t-a.u-a " +
      " 3asd_@a Q4@Z3--4.434.234 myemail@192.168.1.100\t sashasochka@gmail.com \n"
    assert(EmailDFA matches str)
    assert(EmailDFA.matchStrings(str).get === List(
      "a3_omg2", "gmailfd.sfd.jk.com", "adfdf_3f", "m-e-t-a.u-a", "3asd_", "a",
      "Q4", "Z3--4.434.234", "myemail", "192.168.1.100", "sashasochka", "gmail.com"
    ))
  }

  it should "not match email with subdomain starting/ending with dash" in {
    assert(EmailDFA nonMatches "hello@omg-.com")
    assert(EmailDFA nonMatches  "hello@omg.-com")
    assert((EmailDFA matchStrings "hello@omg.-com").isEmpty)
  }

  it should "not match email without domain" in {
    assert(EmailDFA nonMatches "hello@")
  }

  it should "not match email without username" in {
    assert(EmailDFA nonMatches "@omg.omg")
  }

  it should "not match email ending with dot" in {
    assert(EmailDFA nonMatches "hello@omg.")
  }
}
