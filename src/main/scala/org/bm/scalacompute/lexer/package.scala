/*
 *     Copyright (C) 2015  morinb
 *     https://github.com/morinb
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.bm.scalacompute

import scala.language.implicitConversions

/**
 *
 * @author morinb.
 */
package object lexer {

  object Implicits {
    implicit def constantToDouble(c: Constant): Double = c.doubleValue
  }

  trait Token

  trait Associative {
    def leftAssociative: Boolean
  }

  trait LeftAssociative extends Associative {
    def leftAssociative: Boolean = true
  }

  trait RightAssociative extends Associative {
    def leftAssociative: Boolean = false
  }

  trait Argumented {
    def argsNumber: Int
  }

  trait MonoArgument extends Argumented {
    def argsNumber = 1
  }

  trait DualArguments extends Argumented {
    def argsNumber = 2
  }

  trait Precedence {
    def precedence: Int
  }

  trait PrecedenceForUnary extends Precedence {
    def precedence: Int = 14
  }

  trait PrecedenceMulDivMod extends Precedence {
    def precedence: Int = 13
  }

  trait PrecedenceAdditonSubtract extends Precedence {
    def precedence: Int = 12
  }


  trait Executable {
    def calc(operatorName: String, operands: Seq[String], requiredArgsNumber: Int)(exec: (Seq[String]) => String): String = {
      require(operands.length == requiredArgsNumber, s"Operator $operatorName requires $requiredArgsNumber operand${if (requiredArgsNumber > 1) "s"}")
      exec(operands)
    }

    def executeMethod: (Seq[String]) => String
  }


  trait Value {
    val value: String
  }

  trait DoubleValue {
    val doubleValue: Double
  }

  trait PossibleNames {
    def possibleNamesIgnoringCase: Seq[String]
  }

  trait Function extends Token with Argumented with Executable with PossibleNames

  trait Operator extends Token with Associative with Argumented with Precedence with Executable with PossibleNames

  trait Constant extends Token with PossibleNames with Value with DoubleValue {
    self =>
    override lazy val value: String = self.doubleValue.toString
  }

  object Functions {
    private[this] var functions = List[Function]()

    def addFunctions(fs: Function*): Unit = {
      fs foreach (f => functions = f :: functions)
    }

    def getFunctions: Seq[Function] = functions

    case class SQRT() extends Function with MonoArgument with PossibleNames  {
      self =>
      override def executeMethod: (Seq[String]) => String =
        calc(self.getClass.getName, _, argsNumber)(Seq => Math.sqrt(Seq.head.toDouble).toString)

      override def possibleNamesIgnoringCase: Seq[String] = Seq("sqrt")
    }

    case class LOG() extends Function with MonoArgument with PossibleNames  {
      self =>
      override def executeMethod: (Seq[String]) => String =
        calc(self.getClass.getName, _, argsNumber)(Seq => Math.log10(Seq.head.toDouble).toString)

      override def possibleNamesIgnoringCase: Seq[String] = Seq("log")
    }

    case class EXP() extends Function with MonoArgument with PossibleNames  {
      self =>
      override def executeMethod: (Seq[String]) => String =
        calc(self.getClass.getName, _, argsNumber)(Seq => Math.exp(Seq.head.toDouble).toString)

      override def possibleNamesIgnoringCase: Seq[String] = Seq("exp")
    }

  }

  object Operators {

    private[this] var operators = List[Operator]()

    def addOperators(os: Operator*): Unit = {
      os foreach (o => operators = o :: operators)
    }

    def getOperators: Seq[Operator] = operators

    addOperators(PLUS(), MINUS(), TIMES(), DIVIDE(), NEGATE(), MODULO(), POWER())


    implicit def enrichDouble(d: Double): RichDouble = new RichDouble(d)

    class RichDouble(d: Double) {
      def **(other: Double): Double = {
        Math.pow(d, other)
      }
    }

    case class PLUS() extends Operator with LeftAssociative with DualArguments with PrecedenceAdditonSubtract with PossibleNames {
      self =>
      override def executeMethod: (Seq[String]) => String =
        calc(self.getClass.getName, _, argsNumber)(_.map(_.toDouble).sum.toString)

      override def possibleNamesIgnoringCase: Seq[String] = Seq("+")
    }

    case class MINUS() extends Operator with LeftAssociative with DualArguments with PrecedenceAdditonSubtract with PossibleNames {
      self =>

      override def executeMethod: (Seq[String]) => String =
        calc(self.getClass.getName, _, argsNumber)(_.map(_.toDouble).foldRight(0.0)(_ - _).toString)

      override def possibleNamesIgnoringCase: Seq[String] = Seq("-")
    }

    case class TIMES() extends Operator with DualArguments with LeftAssociative with PrecedenceMulDivMod with PossibleNames {
      self =>

      override def executeMethod: (Seq[String]) => String =
        calc(self.getClass.getName, _, argsNumber)(_.map(_.toDouble).foldLeft(1.0)(_ * _).toString)

      override def possibleNamesIgnoringCase: Seq[String] = Seq("*")
    }

    case class DIVIDE() extends Operator with LeftAssociative with DualArguments with PrecedenceMulDivMod with PossibleNames {
      self =>

      override def executeMethod: (Seq[String]) => String =
        calc(self.getClass.getName, _, argsNumber)(_.map(_.toDouble).foldRight(1.0)(_ / _).toString)

      override def possibleNamesIgnoringCase: Seq[String] = Seq("/")
    }

    case class NEGATE() extends Operator with LeftAssociative with MonoArgument with PrecedenceForUnary with PossibleNames {
      self =>

      override def executeMethod: (Seq[String]) => String =
        calc(self.getClass.getName, _, argsNumber)(Seq => (-Seq.head.toDouble).toString)

      override def possibleNamesIgnoringCase: Seq[String] = Seq("-")
    }

    case class MODULO() extends Operator with LeftAssociative with DualArguments with PrecedenceMulDivMod with PossibleNames {
      self =>
      override def executeMethod: (Seq[String]) => String =
        calc(self.getClass.getName, _, argsNumber) { Seq =>
          val op = Seq.map(_.toDouble)
          (op(0) % op(1)).toString
        }

      override def possibleNamesIgnoringCase: Seq[String] = Seq("%")
    }

    case class POWER() extends Operator with RightAssociative with DualArguments with PrecedenceForUnary with PossibleNames {
      self =>

      override def executeMethod: (Seq[String]) => String =
        calc(self.getClass.getName, _, argsNumber)(_.map(_.toDouble).foldRight(1.0)(_ ** _).toString) // use implicit to convert to RichDouble which defines ** method

      override def possibleNamesIgnoringCase: Seq[String] = Seq("^")
    }


  }

  object Constants {
    private[this] var constants = List[Constant]()

    def addConstants(cs: Constant*): Unit = {
      cs foreach (c => constants = c :: constants)
    }

    def getConstants: Seq[Constant] = constants

    addConstants(PI(), PHI(), GAMMA())

    case class PI() extends Constant {
      override def possibleNamesIgnoringCase: Seq[String] = List("pi")

      override val doubleValue: Double = Math.PI
    }

    case class E() extends Constant {
      override def possibleNamesIgnoringCase: Seq[String] = List("e")

      override val doubleValue: Double = Math.E
    }

    case class PHI() extends Constant {
      override val doubleValue: Double = (1 + Math.sqrt(5)) / 2

      override def possibleNamesIgnoringCase: Seq[String] = List("phi", "?", "?", "?")
    }

    case class GAMMA() extends Constant {
      override val doubleValue: Double = 0.5772156649015329

      override def possibleNamesIgnoringCase: Seq[String] = List("gamma", "?")

    }

  }

}
