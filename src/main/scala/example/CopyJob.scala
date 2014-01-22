package example

import com.twitter.scalding._
import org.apache.hadoop.util.ToolRunner
import org.apache.hadoop.conf.Configuration

class CopyJob(args : Args) extends Job(args) {
  TextLine(args("input"))
    .write(Tsv(args("output")))
}

object CopyJobRunner {
  def main(args : Array[String]) {
    val tool = new Tool {
      override def getJob(args: Args) = new CopyJob(args)
    }
    ToolRunner.run(new Configuration, tool, args)
  }
}
