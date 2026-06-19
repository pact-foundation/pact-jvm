import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

class GenerateDoctestsTask extends DefaultTask {

    @InputFile
    File readmeFile

    @Input
    String basePackage

    @Internal
    File javaOutputDir

    @Internal
    File kotlinOutputDir

    GenerateDoctestsTask() {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    void generate() {
        def blocks = DoctestUtils.extractCodeBlocks(readmeFile)

        blocks.each { Map block ->
            def isKotlin = block.lang == 'kotlin'
            def outputDir = isKotlin ? kotlinOutputDir : javaOutputDir
            def id = block.id as String
            def className = DoctestUtils.classNameFor(block.lang as String, id)
            def ext = DoctestUtils.fileExtFor(block.lang as String)
            def outFile = new File(outputDir, "${className}.${ext}")
            def marker = "${readmeFile.name}:${block.lang}:${id}"
            def codeContent = (block.content as List<String>).join('\n')

            if (!outFile.exists()) {
                outputDir.mkdirs()
                outFile.write(generateStub(block.lang as String, className, marker, codeContent, id), 'UTF-8')
                logger.lifecycle("Created: ${outFile.path}")
            } else if (updateMarkerRegion(outFile, marker, block.blockNum as int, block.lang as String, codeContent)) {
                logger.lifecycle("Updated: ${outFile.path}")
            }
        }
    }

    boolean updateMarkerRegion(File file, String marker, int blockNum, String lang, String newContent) {
        def text = DoctestUtils.normalizeLineEndings(file.text)
        def beginTag = "// @DOCTEST-BEGIN ${marker}"
        def endTag = '// @DOCTEST-END'

        def beginIdx = text.indexOf(beginTag)

        // If new-style marker not found, try migrating from old positional marker format
        if (beginIdx < 0) {
            def oldMarker = "${readmeFile.name}:${lang}:${blockNum}"
            def oldBeginTag = "// @DOCTEST-BEGIN ${oldMarker}"
            def oldBeginIdx = text.indexOf(oldBeginTag)
            if (oldBeginIdx >= 0) {
                text = text.substring(0, oldBeginIdx) + beginTag + text.substring(oldBeginIdx + oldBeginTag.length())
                beginIdx = oldBeginIdx
                logger.lifecycle("Migrated marker in ${file.name}: ${oldMarker} -> ${marker}")
            }
        }

        if (beginIdx < 0) return false
        def afterBegin = beginIdx + beginTag.length()
        def endIdx = text.indexOf(endTag, afterBegin)
        if (endIdx < 0) return false

        // Detect indentation of the end-tag line
        def lineStart = text.lastIndexOf('\n', endIdx) + 1
        def indent = new StringBuilder()
        def i = lineStart
        while (i < endIdx && text.charAt(i) in [' ' as char, '\t' as char]) {
            indent.append(text.charAt(i))
            i++
        }

        def formatted = '\n' + newContent.split('\n').collect { "${indent}${it}" }.join('\n') + '\n' + indent
        def existing = text.substring(afterBegin, endIdx)
        if (existing == formatted && text.contains(beginTag)) return false

        file.write(text.substring(0, afterBegin) + formatted + text.substring(endIdx), 'UTF-8')
        true
    }

    String generateStub(String lang, String className, String marker, String content, String id) {
        def pkg = "${basePackage}.doctest"
        def indent = '        '
        def indented = content.split('\n').collect { "${indent}${it}" }.join('\n')
        def readmeName = readmeFile.name

        if (lang == 'kotlin') {
            return """\
// Auto-generated — run './gradlew generateDoctests' to regenerate from ${readmeName}
// Source: ${readmeName} block ${id}
// Remove @Disabled once the test compiles and passes
package ${pkg}

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
// TODO: add required imports

@Disabled("Doctest stub — see ${readmeName} block ${id}")
class ${className} {

    @Test
    fun block() {
        // @DOCTEST-BEGIN ${marker}
${indented}
        // @DOCTEST-END
    }
}
"""
        }

        """\
// Auto-generated — run './gradlew generateDoctests' to regenerate from ${readmeName}
// Source: ${readmeName} block ${id}
// Remove @Disabled once the test compiles and passes
package ${pkg};

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see ${readmeName} block ${id}")
class ${className} {

    @Test
    void block() throws Exception {
        // @DOCTEST-BEGIN ${marker}
${indented}
        // @DOCTEST-END
    }
}
"""
    }
}