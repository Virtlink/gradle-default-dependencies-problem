package mb.releng.eclipse.model

import org.w3c.dom.Node
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory


data class Site(
  val dependencies: Collection<FeatureDependency>
) {
  companion object {
    fun read(file: Path): Site {
      return Files.newInputStream(file).buffered().use {
        read(it)
      }
    }

    fun read(inputStream: InputStream): Site {
      val factory = DocumentBuilderFactory.newInstance()
      val builder = factory.newDocumentBuilder()
      val doc = builder.parse(inputStream)

      val siteNode = doc.firstChild ?: error("Cannot parse site XML; no root node")
      val featureNodes = siteNode.childNodes
      val dependencies = mutableListOf<FeatureDependency>()
      for(i in 0 until featureNodes.length) {
        val featureNode = featureNodes.item(i)
        if(featureNode.nodeType != Node.ELEMENT_NODE) continue
        val depId = featureNode.attributes.getNamedItem("id")?.nodeValue
          ?: error("Cannot parse site XML; feature node has no 'id' attribute")
        val depVersionStr = featureNode.attributes.getNamedItem("version")?.nodeValue
          ?: error("Cannot parse site XML; feature node has no 'version' attribute")
        val depVersion = Version.parse(depVersionStr)
          ?: error("Cannot parse site XML; could not parse version '$depVersionStr'")
        dependencies.add(FeatureDependency(depId, depVersion))
      }

      return Site(dependencies)
    }
  }
}

data class FeatureDependency(
  val id: String,
  val version: Version
)
