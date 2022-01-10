package au.com.dius.pact.core.support.json

import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.StandardCharsets

@SuppressWarnings('LineLength')
class KafkaSchemaRegistryWireFormatterSpec extends Specification {

  @Unroll
  def 'addMagicBytesToString - Adds magic bytes to start of string'() {
    expect:
    KafkaSchemaRegistryWireFormatter.addMagicBytesToString(value) == expected

    where:

    value                           | expected
    '     '                         | getMagicBytesString() + '     '
    '  \t\n\r'                      | getMagicBytesString() + '  \t\n\r'
    '       \t\n\r'                 | getMagicBytesString() + '       \t\n\r'
    '  -'                           | getMagicBytesString() + '  -'
    'Null'                          | getMagicBytesString() + 'Null'
    '     null true'                | getMagicBytesString() + '     null true'
    '     "null true'               | getMagicBytesString() + '     "null true'
    '["null", true'                 | getMagicBytesString() + '["null", true'
    '{"null": true'                 | getMagicBytesString() + '{"null": true'
    '12]'                           | getMagicBytesString() + '12]'
    'true}'                         | getMagicBytesString() + 'true}'
    '1234,'                         | getMagicBytesString() + '1234,'
    '{null: true}'                  | getMagicBytesString() + '{null: true}'
    '{"null: true}'                 | getMagicBytesString() + '{"null: true}'
    '{"nu\\ll": true}'              | getMagicBytesString() + '{"nu\\ll": true}'
    '{"null" true}'                 | getMagicBytesString() + '{"null" true}'
    '["null" true]'                 | getMagicBytesString() + '["null" true]'
    '{"null": true "other": false}' | getMagicBytesString() + '{"null": true "other": false}'
  }

  def 'addMagicBytesToString - returns empty string when input is empty'() {
    given:
    def value = ''

    when:
    def result = KafkaSchemaRegistryWireFormatter.addMagicBytesToString(value)

    then:
    result == ''
  }

  def 'addMagicBytesToString - returns null when input is null'() {
    given:
    def value = null

    when:
    def result = KafkaSchemaRegistryWireFormatter.addMagicBytesToString(value)

    then:
    result == null
  }

  @Unroll
  def 'addMagicBytes - Adds magic bytes to start of bytes'() {
    expect:
    KafkaSchemaRegistryWireFormatter.addMagicBytes(value) == expected

    where:

    value                                 | expected
    '     '.bytes                         | prependMagicBytes('     '.bytes)
    '  \t\n\r'.bytes                      | prependMagicBytes('  \t\n\r'.bytes)
    '       \t\n\r'.bytes                 | prependMagicBytes('       \t\n\r'.bytes)
    '  -'.bytes                           | prependMagicBytes('  -'.bytes)
    'Null'.bytes                          | prependMagicBytes('Null'.bytes)
    '     null true'.bytes                | prependMagicBytes('     null true'.bytes)
    '     "null true'.bytes               | prependMagicBytes('     "null true'.bytes)
    '["null", true'.bytes                 | prependMagicBytes('["null", true'.bytes)
    '{"null": true'.bytes                 | prependMagicBytes('{"null": true'.bytes)
    '12]'.bytes                           | prependMagicBytes('12]'.bytes)
    'true}'.bytes                         | prependMagicBytes('true}'.bytes)
    '1234,'.bytes                         | prependMagicBytes('1234,'.bytes)
    '{null: true}'.bytes                  | prependMagicBytes('{null: true}'.bytes)
    '{"null: true}'.bytes                 | prependMagicBytes('{"null: true}'.bytes)
    '{"nu\\ll": true}'.bytes              | prependMagicBytes('{"nu\\ll": true}'.bytes)
    '{"null" true}'.bytes                 | prependMagicBytes('{"null" true}'.bytes)
    '["null" true]'.bytes                 | prependMagicBytes('["null" true]'.bytes)
    '{"null": true "other": false}'.bytes | prependMagicBytes('{"null": true "other": false}'.bytes)
  }

  def 'addMagicBytes - returns empty bytes when input is empty'() {
    given:
    def value = new byte[]{}

    when:
    def result = KafkaSchemaRegistryWireFormatter.addMagicBytes(value)

    then:
    result.length == 0
  }

  def 'addMagicBytes - returns null when input is null'() {
    given:
    def value = null

    when:
    def result = KafkaSchemaRegistryWireFormatter.addMagicBytes(value)

    then:
    result.length == 0
  }

  def 'removeMagicBytes - removes the length of the magic bytes from the start of the input'() {
    expect:
    KafkaSchemaRegistryWireFormatter.removeMagicBytes(value) == expected

    where:

    value                                 | expected
    '     '.bytes                         | removeMagicBytesLength('     '.bytes)
    '  \t\n\r'.bytes                      | removeMagicBytesLength('  \t\n\r'.bytes)
    '       \t\n\r'.bytes                 | removeMagicBytesLength('       \t\n\r'.bytes)
    '  -'.bytes                           | removeMagicBytesLength('  -'.bytes)
    'Null'.bytes                          | removeMagicBytesLength('Null'.bytes)
    '     null true'.bytes                | removeMagicBytesLength('     null true'.bytes)
    '     "null true'.bytes               | removeMagicBytesLength('     "null true'.bytes)
    '["null", true'.bytes                 | removeMagicBytesLength('["null", true'.bytes)
    '{"null": true'.bytes                 | removeMagicBytesLength('{"null": true'.bytes)
    '12]'.bytes                           | removeMagicBytesLength('12]'.bytes)
    'true}'.bytes                         | removeMagicBytesLength('true}'.bytes)
    '1234,'.bytes                         | removeMagicBytesLength('1234,'.bytes)
    '{null: true}'.bytes                  | removeMagicBytesLength('{null: true}'.bytes)
    '{"null: true}'.bytes                 | removeMagicBytesLength('{"null: true}'.bytes)
    '{"nu\\ll": true}'.bytes              | removeMagicBytesLength('{"nu\\ll": true}'.bytes)
    '{"null" true}'.bytes                 | removeMagicBytesLength('{"null" true}'.bytes)
    '["null" true]'.bytes                 | removeMagicBytesLength('["null" true]'.bytes)
    '{"null": true "other": false}'.bytes | removeMagicBytesLength('{"null": true "other": false}'.bytes)
  }

  def 'removeMagicBytes - returns null when input is null'() {
    given:
    def value = null

    when:
    def result = KafkaSchemaRegistryWireFormatter.removeMagicBytes(value)

    then:
    result == null
  }

  def removeMagicBytesLength(byte[] value) {
    int magicBytesLength = getMagicBytes().length
    int valueLength = value.length

    if(magicBytesLength < valueLength) {
      return Arrays.copyOfRange(value, magicBytesLength, valueLength)
    }
    return new byte[]{}
  }

  def prependMagicBytes(byte[] value) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
    outputStream.write(getMagicBytes())
    outputStream.write(value)
    return outputStream.toByteArray()
  }

  def getMagicBytesString() {
    return new String(getMagicBytes(), StandardCharsets.UTF_8)
  }

  def getMagicBytes() {
    return new byte[]{0x00, 0x00, 0x00, 0x00, 0x01}
  }
}
