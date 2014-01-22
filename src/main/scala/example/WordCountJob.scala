package example

import com.twitter.scalding._
import org.apache.hadoop.util.ToolRunner
import org.apache.hadoop.conf.Configuration

class WordCountJob(args : Args) extends Job(args) {
  TextLine(args("input"))
    .flatMap('line -> 'word) { line : String ⇒ tokenize(line) }
    .groupBy('word) { _.size }
    .groupAll { _.sortBy('size).reverse }
    .write(Tsv(args("output")))

  // Split a piece of text into individual words.
  def tokenize(text : String) : Array[String] = {
    // Lowercase each word and remove punctuation.
    text.toLowerCase.replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+").filter(_ != "")
  }

  override def config: Map[AnyRef, AnyRef] = {
    super.config ++ Map("cascading.app.appjar.class" -> classOf[WordCountJob])
  }
}

object WordCountJobRunner {
  def main(args : Array[String]) {
    val tool = new Tool {
      override def getJob(args: Args) = new WordCountJob(args)
    }
    ToolRunner.run(new Configuration, tool, args)
  }
}
