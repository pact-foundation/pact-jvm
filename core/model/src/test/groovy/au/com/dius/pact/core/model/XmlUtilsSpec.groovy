package au.com.dius.pact.core.model

import org.w3c.dom.Element
import spock.lang.Shared
import spock.lang.Specification

@SuppressWarnings('LineLength')
class XmlUtilsSpec extends Specification {
  @Shared
  static xml = '''<?xml version="1.0" encoding="UTF-8"?>
    <config>
      <name>My Settings</name>
      <sound>
        <property name="volume" value="11" />
        <property name="mixer" value="standard" />
      </sound>
    </config>
  '''
  @Shared
  static root = XmlUtils.INSTANCE.parse(xml) as Element

  def 'resolve path test'() {
    expect:
    XmlUtils.INSTANCE.resolvePath(root, path) == result

    where:

    path                                            | result
    DocPath.root()                                  | []
    new DocPath('$.config')                         | ['/config[0]']
    new DocPath('$.config.sound')                   | ['/config[0]/sound[0]']
    new DocPath('$.config.sound.property')          | ['/config[0]/sound[0]/property[0]', '/config[0]/sound[0]/property[1]']
    new DocPath('$.config.sound[0].property[0]')    | ['/config[0]/sound[0]/property[0]']
    new DocPath('$.config.*')                       | ['/config[0]/name[0]', '/config[0]/sound[0]']
    new DocPath('$.config[*]')                      | ['/config[0]/name[0]', '/config[0]/sound[0]']
    new DocPath('$.config.sound.property.@name')    | ['/config[0]/sound[0]/property[0]/@name', '/config[0]/sound[0]/property[1]/@name']
    new DocPath('$.config.sound.property.@other')   | []
    new DocPath('$.config.sound.*.@name')           | ['/config[0]/sound[0]/property[0]/@name', '/config[0]/sound[0]/property[1]/@name']
    new DocPath('$.config.name.#text')              | ['/config[0]/name[0]/#text']
    new DocPath('$.config.*.#text')                 | ['/config[0]/name[0]/#text']
    new DocPath('$.config.sound.property.#text')    | []
    new DocPath('$.config.sound.property[1].@name') | ['/config[0]/sound[0]/property[1]/@name']
    new DocPath('$.config.sound.property[2].@name') | []
  }

  def 'resolve matching node test'() {
    given:
    def sound = root.getElementsByTagName('sound').item(0) as Element
    def propertiesList = sound.getElementsByTagName('property')
    def properties = (0..<propertiesList.length).step(1).collect { propertiesList.item(it) }

    when:
    def result = XmlUtils.INSTANCE.resolveMatchingNode(root, '/config[0]')

    then:
    result == new XmlResult.ElementNode(root)

    when:
    result = XmlUtils.INSTANCE.resolveMatchingNode(root, '/config[1]')

    then:
    result == null

    when:
    result = XmlUtils.INSTANCE.resolveMatchingNode(root, '/config[0]/sound[0]')

    then:
    result == new XmlResult.ElementNode(sound)

    when:
    result = XmlUtils.INSTANCE.resolveMatchingNode(root, '/config[0]/sound[1]')

    then:
    result == null

    when:
    result = XmlUtils.INSTANCE.resolveMatchingNode(root, '/config[0]/sound[0]/property[0]')

    then:
    result == new XmlResult.ElementNode(properties[0])

    when:
    result = XmlUtils.INSTANCE.resolveMatchingNode(root, '/config[0]/sound[0]/property[1]')

    then:
    result == new XmlResult.ElementNode(properties[1])

    when:
    result = XmlUtils.INSTANCE.resolveMatchingNode(root, '/config[0]/sound[0]/property[0]/@name')

    then:
    result == new XmlResult.Attribute('name', 'volume')

    when:
    result = XmlUtils.INSTANCE.resolveMatchingNode(root, '/config[0]/sound[0]/property[1]/@name')

    then:
    result == new XmlResult.Attribute('name', 'mixer')

    when:
    result = XmlUtils.INSTANCE.resolveMatchingNode(root, '/config[0]/sound[0]/property[1]/@other')

    then:
    result == null

    when:
    result = XmlUtils.INSTANCE.resolveMatchingNode(root, '/config[0]/name[0]/#text')

    then:
    result == new XmlResult.TextNode('My Settings')

    when:
    result = XmlUtils.INSTANCE.resolveMatchingNode(root, '/config[0]/sound[0]/property[0]/#text')

    then:
    result == null

    when:
    result = XmlUtils.INSTANCE.resolveMatchingNode(root, '/config[0]/#text')

    then:
    result == null
  }
}
