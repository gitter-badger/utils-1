package fi.sn127.utils.testing

@SuppressWarnings(Array(
  "org.wartremover.warts.Equals"))
object TestComparator   {

  def txtComparator(first: String, second: String) :Boolean = {
    val srcFirst = scala.io.Source.fromFile(first)
    val txtFirst = try srcFirst.getLines mkString "\n" finally srcFirst.close()

    val srcSecond = scala.io.Source.fromFile(second)
    val txtSecond = try srcSecond.getLines mkString "\n" finally srcSecond.close()

    txtFirst == txtSecond
  }

  /*
  extends FlatSpec
import org.scalatest.FlatSpec
import java.io.File

  def xmlComparator(first: String, second: String) :Boolean = {
    import org.scalatest.StreamlinedXmlEquality._

    //println("xml: " + first + " " + second)

    val xmlFirst = scala.xml.XML.loadFile(first)
    val xmlSecond = scala.xml.XML.loadFile(second)

    xmlFirst === xmlSecond
  }
  */
}

final case class TestVec(output: String, reference: String, comparator: (String, String) => Boolean)

final case class TestCase(conf: String, args: List[Array[String]], testVec: List[TestVec])

