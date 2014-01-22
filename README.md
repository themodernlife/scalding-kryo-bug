Kryo Bug
========

There is something really weird with how Kryo is serializing functions.

This project has 2 really simple jobs

```scala
class CopyJob(args : Args) extends Job(args) {
  TextLine(args("input"))
    .write(Tsv(args("output")))
}
```

```scala
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
}
```

All scalding dependencies (but note, not the actual job classes themselves) have been assembled into an überjar in `deps/scalding-and-deps.jar`.

This job works fine

```bash
HADOOP_CLASSPATH=$PWD/deps/scalding-and-deps.jar hadoop jar target/scala-2.10/scalding-example_2.10-0.1-SNAPSHOT.jar example.CopyJobRunner --hdfs --input src/test/resources/alice.txt --output target/word-count
```

As expected, it can find everything it needs on the classpath: scalding and dependencies from the jar in `HADOOP_CLASSPATH` and the actual job classes via the jar passed as an argument to `hadoop`

Curiously, though, the word count job *fails*!  It seems to be because the anonymous functions are not being found inside of Hadoop's classloader??

```bash
HADOOP_CLASSPATH=$PWD/deps/scalding-and-deps.jar hadoop jar target/scala-2.10/scalding-example_2.10-0.1-SNAPSHOT.jar example.WordCountJobRunner --hdfs --input src/test/resources/alice.txt --output target/word-count
...
java.lang.Exception: java.lang.RuntimeException: Error in configuring object
	at org.apache.hadoop.mapred.LocalJobRunner$Job.run(LocalJobRunner.java:354)
Caused by: java.lang.RuntimeException: Error in configuring object
	at org.apache.hadoop.util.ReflectionUtils.setJobConf(ReflectionUtils.java:93)
	at org.apache.hadoop.util.ReflectionUtils.setConf(ReflectionUtils.java:64)
	at org.apache.hadoop.util.ReflectionUtils.newInstance(ReflectionUtils.java:117)
	at org.apache.hadoop.mapred.MapTask.runOldMapper(MapTask.java:426)
	at org.apache.hadoop.mapred.MapTask.run(MapTask.java:366)
	at org.apache.hadoop.mapred.LocalJobRunner$Job$MapTaskRunnable.run(LocalJobRunner.java:223)
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:471)
	at java.util.concurrent.FutureTask.run(FutureTask.java:262)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615)
	at java.lang.Thread.run(Thread.java:744)
Caused by: java.lang.reflect.InvocationTargetException
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at org.apache.hadoop.util.ReflectionUtils.setJobConf(ReflectionUtils.java:88)
	... 10 more
Caused by: cascading.flow.FlowException: internal error during mapper configuration
	at cascading.flow.hadoop.FlowMapper.configure(FlowMapper.java:99)
	... 15 more
Caused by: com.esotericsoftware.kryo.KryoException: Unable to find class: example.WordCountJob$$anonfun$3
	at com.esotericsoftware.kryo.util.DefaultClassResolver.readName(DefaultClassResolver.java:138)
	at com.esotericsoftware.kryo.util.DefaultClassResolver.readClass(DefaultClassResolver.java:115)
	at com.esotericsoftware.kryo.Kryo.readClass(Kryo.java:610)
	at com.esotericsoftware.kryo.Kryo.readClassAndObject(Kryo.java:721)
	at com.twitter.chill.SomeSerializer.read(SomeSerializer.scala:25)
	at com.twitter.chill.SomeSerializer.read(SomeSerializer.scala:19)
	at com.esotericsoftware.kryo.Kryo.readClassAndObject(Kryo.java:729)
	at com.twitter.chill.SerDeState.readClassAndObject(SerDeState.java:61)
	at com.twitter.chill.KryoPool.fromBytes(KryoPool.java:94)
	at com.twitter.chill.Externalizer.fromBytes(Externalizer.scala:149)
	at com.twitter.chill.Externalizer.maybeReadJavaKryo(Externalizer.scala:162)
	at com.twitter.chill.Externalizer.readExternal(Externalizer.scala:152)
	at java.io.ObjectInputStream.readExternalData(ObjectInputStream.java:1837)
	at java.io.ObjectInputStream.readOrdinaryObject(ObjectInputStream.java:1796)
	at java.io.ObjectInputStream.readObject0(ObjectInputStream.java:1350)
	at java.io.ObjectInputStream.defaultReadFields(ObjectInputStream.java:1990)
	at java.io.ObjectInputStream.readSerialData(ObjectInputStream.java:1915)
	at java.io.ObjectInputStream.readOrdinaryObject(ObjectInputStream.java:1798)
	at java.io.ObjectInputStream.readObject0(ObjectInputStream.java:1350)
	at java.io.ObjectInputStream.defaultReadFields(ObjectInputStream.java:1990)
	at java.io.ObjectInputStream.readSerialData(ObjectInputStream.java:1915)
	at java.io.ObjectInputStream.readOrdinaryObject(ObjectInputStream.java:1798)
	at java.io.ObjectInputStream.readObject0(ObjectInputStream.java:1350)
	at java.io.ObjectInputStream.defaultReadFields(ObjectInputStream.java:1990)
	at java.io.ObjectInputStream.readSerialData(ObjectInputStream.java:1915)
	at java.io.ObjectInputStream.readOrdinaryObject(ObjectInputStream.java:1798)
	at java.io.ObjectInputStream.readObject0(ObjectInputStream.java:1350)
	at java.io.ObjectInputStream.readObject(ObjectInputStream.java:370)
	at java.util.HashMap.readObject(HashMap.java:1184)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:57)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:606)
	at java.io.ObjectStreamClass.invokeReadObject(ObjectStreamClass.java:1017)
	at java.io.ObjectInputStream.readSerialData(ObjectInputStream.java:1893)
	at java.io.ObjectInputStream.readOrdinaryObject(ObjectInputStream.java:1798)
	at java.io.ObjectInputStream.readObject0(ObjectInputStream.java:1350)
	at java.io.ObjectInputStream.defaultReadFields(ObjectInputStream.java:1990)
	at java.io.ObjectInputStream.readSerialData(ObjectInputStream.java:1915)
	at java.io.ObjectInputStream.readOrdinaryObject(ObjectInputStream.java:1798)
	at java.io.ObjectInputStream.readObject0(ObjectInputStream.java:1350)
	at java.io.ObjectInputStream.defaultReadFields(ObjectInputStream.java:1990)
	at java.io.ObjectInputStream.readSerialData(ObjectInputStream.java:1915)
	at java.io.ObjectInputStream.readOrdinaryObject(ObjectInputStream.java:1798)
	at java.io.ObjectInputStream.readObject0(ObjectInputStream.java:1350)
	at java.io.ObjectInputStream.readObject(ObjectInputStream.java:370)
	at cascading.flow.hadoop.util.JavaObjectSerializer.deserialize(JavaObjectSerializer.java:101)
	at cascading.flow.hadoop.util.HadoopUtil.deserializeBase64(HadoopUtil.java:295)
	at cascading.flow.hadoop.util.HadoopUtil.deserializeBase64(HadoopUtil.java:276)
	at cascading.flow.hadoop.FlowMapper.configure(FlowMapper.java:80)
	... 15 more
Caused by: java.lang.ClassNotFoundException: example.WordCountJob$$anonfun$3
	at java.net.URLClassLoader$1.run(URLClassLoader.java:366)
	at java.net.URLClassLoader$1.run(URLClassLoader.java:355)
	at java.security.AccessController.doPrivileged(Native Method)
	at java.net.URLClassLoader.findClass(URLClassLoader.java:354)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:425)
	at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:308)
	at java.lang.ClassLoader.loadClass(ClassLoader.java:358)
	at java.lang.Class.forName0(Native Method)
	at java.lang.Class.forName(Class.java:270)
	at com.esotericsoftware.kryo.util.DefaultClassResolver.readName(DefaultClassResolver.java:136)
	... 64 more

```
