package mb.releng.eclipse.model.eclipse

import org.w3c.dom.Node
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory


data class Feature(
  val id: String,
  val version: BundleVersion,
  val label: String?,
  val dependencies: Collection<Dependency>
) {
  data class Dependency(
    val coordinates: Coordinates,
    val unpack: Boolean
  ) {
    data class Coordinates(val id: String, val version: BundleVersion)
  }


  companion object {
    fun read(file: Path): Feature {
      return Files.newInputStream(file).buffered().use {
        read(it)
      }
    }

    fun read(inputStream: InputStream): Feature {
      val factory = DocumentBuilderFactory.newInstance()
      val builder = factory.newDocumentBuilder()
      val doc = builder.parse(inputStream)

      val featureNode = doc.firstChild ?: error("Cannot parse feature XML; no root node")
      val id = featureNode.attributes.getNamedItem("id")?.nodeValue
        ?: error("Cannot parse feature XML; root feature node has no 'id' attribute")
      val versionStr = featureNode.attributes.getNamedItem("version")?.nodeValue
        ?: error("Cannot parse feature XML; root feature node has no 'version' attribute")
      val version = BundleVersion.parse(versionStr)
        ?: error("Cannot parse feature XML; could not parse version '$versionStr'")
      val label = featureNode.attributes.getNamedItem("label")?.nodeValue

      val subNodes = featureNode.childNodes
      val dependencies = mutableListOf<Dependency>()
      for(i in 0 until subNodes.length) {
        val subNode = subNodes.item(i)
        if(subNode.nodeType != Node.ELEMENT_NODE) continue
        // TODO: full support for feature.xml. See: https://help.eclipse.org/photon/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Freference%2Fmisc%2Ffeature_manifest.html.
        if(subNode.nodeName != "plugin") continue
        val depId = subNode.attributes.getNamedItem("id")?.nodeValue
          ?: error("Cannot parse feature XML; plugin node has no 'id' attribute")
        val depVersionStr = subNode.attributes.getNamedItem("version")?.nodeValue
          ?: error("Cannot parse feature XML; plugin node has no 'version' attribute")
        val depVersion = BundleVersion.parse(depVersionStr)
          ?: error("Cannot parse feature XML; could not parse version '$depVersionStr'")
        val coordinates = Dependency.Coordinates(depId, depVersion)
        val unpack = subNode.attributes.getNamedItem("unpack")?.nodeValue?.toBoolean() ?: false
        dependencies.add(Dependency(coordinates, unpack))
      }

      return Feature(id, version, label, dependencies)
    }
  }
}