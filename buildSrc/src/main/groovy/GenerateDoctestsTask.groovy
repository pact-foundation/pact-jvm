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

        blocks.eachWithIndex { Map block, int idx ->
            def blockNum = idx + 1
            def isKotlin = block.lang == 'kotlin'
            def outputDir = isKotlin ? kotlinOutputDir : javaOutputDir
            def className = DoctestUtils.classNameFor(block.lang as String, blockNum)
            def ext = DoctestUtils.fileExtFor(block.lang as String)
            def outFile = new File(outputDir, "${className}.${ext}")
            def marker = "${readmeFile.name}:${block.lang}:${blockNum}"
            def codeContent = (block.content as List<String>).join('\n')

            if (!outFile.exists()) {
                outputDir.mkdirs()
                outFile.text = generateStub(block.lang as String, className, marker, codeContent, blockNum)
                logger.lifecycle("Created: ${outFile.path}")
            } else if (updateMarkerRegion(outFile, marker, codeContent)) {
                logger.lifecycle("Updated: ${outFile.path}")
            }
        }
    }

    boolean updateMarkerRegion(File file, String marker, String newContent) {
        def text = file.text
        def beginTag = "// @DOCTEST-BEGIN ${marker}"
        def endTag = '// @DOCTEST-END'

        def beginIdx = text.indexOf(beginTag)
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
        if (existing == formatted) return false

        file.text = text.substring(0, afterBegin) + formatted + text.substring(endIdx)
        true
    }

    String generateStub(String lang, String className, String marker, String content, int blockNum) {
        def pkg = "${basePackage}.doctest"
        def indent = '        '
        def indented = content.split('\n').collect { "${indent}${it}" }.join('\n')
        def readmeName = readmeFile.name

        if (lang == 'kotlin') {
            return """\
// Auto-generated — run './gradlew generateDoctests' to regenerate from ${readmeName}
// Source: ${readmeName} block ${blockNum}
// Remove @Disabled once the test compiles and passes
package ${pkg}

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
// TODO: add required imports

@Disabled("Doctest stub — see ${readmeName} block ${blockNum}")
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
// Source: ${readmeName} block ${blockNum}
// Remove @Disabled once the test compiles and passes
package ${pkg};

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
// TODO: add required imports

@Disabled("Doctest stub — see ${readmeName} block ${blockNum}")
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
