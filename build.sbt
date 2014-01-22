import AssemblyKeys._

assemblySettings


organization := "net.themodernlife"

name := "scalding-example"

version := "0.1-SNAPSHOT"


scalaVersion := "2.10.2"

scalacOptions ++= Seq("-encoding", "utf-8", "-deprecation", "-unchecked", "-feature")



resolvers += "Twitter Maven Repository" at "http://maven.twttr.com/"

resolvers += "Concurrent Maven Repo" at "http://conjars.org/repo"



libraryDependencies += "com.twitter" %% "scalding-core" % "0.9.0rc4"

libraryDependencies += "org.apache.hadoop" % "hadoop-core" % "0.20.2"



jarName in assembly <<= (name, version) map { (name, version) ⇒ name + "-" + version + "-deps.jar" }

excludedJars in assembly <<= (fullClasspath in assembly) map { cp ⇒
	cp filter { jar ⇒ Set(
		"jsp-api-2.1-6.1.14.jar",
		"jsp-2.1-6.1.14.jar",
		"jasper-compiler-5.5.12.jar",
		"minlog-1.2.jar",                   // Otherwise causes conflicts with Kyro (which bundles it)
		"janino-2.5.16.jar",                // Janino includes a broken signature, and is not needed anyway
		"commons-beanutils-core-1.8.0.jar", // Clash with each other and with commons-collections
		"commons-beanutils-1.7.0.jar",      // "
		"hadoop-core-0.20.2.jar",           // Provided by Amazon EMR. Delete this line if you're not on EMR
		"hadoop-tools-0.20.2.jar",           // "
		"slf4j-api-1.6.6.jar"
    )(jar.data.getName)}
}
    
mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) ⇒ {
	case "project.clj" ⇒ MergeStrategy.discard // Leiningen build files
	case x             ⇒ old(x)
}}
