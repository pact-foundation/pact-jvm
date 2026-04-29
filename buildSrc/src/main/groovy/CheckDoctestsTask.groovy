import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class CheckDoctestsTask extends DefaultTask {

    @InputFile
    File readmeFile

    @InputFiles
    def doctestFiles = project.files()

    @OutputFile
    File sentinelFile

    @TaskAction
    void check() {
        def blocks = DoctestUtils.extractCodeBlocks(readmeFile)
        def failures = []

        blocks.each { Map block ->
            def id = block.id as String
            def className = DoctestUtils.classNameFor(block.lang as String, id)
            def ext = DoctestUtils.fileExtFor(block.lang as String)
            def marker = "${readmeFile.name}:${block.lang}:${id}"
            def expected = (block.content as List<String>).join('\n')

            def testFile = doctestFiles.files.find { it.name == "${className}.${ext}" }
            if (!testFile) {
                failures << "Block ${id} (${block.lang}): no stub found — run './gradlew generateDoctests'"
                return
            }

            def actual = DoctestUtils.extractMarkerContent(testFile, marker)
            if (actual == null) {
                // Also accept old positional marker format for backward compatibility
                def oldMarker = "${readmeFile.name}:${block.lang}:${block.blockNum}"
                actual = DoctestUtils.extractMarkerContent(testFile, oldMarker)
            }
            if (actual == null) {
                failures << "Block ${id} (${block.lang}): @DOCTEST markers missing in ${testFile.name} — run './gradlew generateDoctests'"
            } else if (DoctestUtils.dedent(actual) != DoctestUtils.dedent(expected)) {
                failures << "Block ${id} (${block.lang}): ${testFile.name} is out of date — run './gradlew generateDoctests'"
            }
        }

        if (!failures.isEmpty()) {
            throw new GradleException(
                "README doctest check failed for ${readmeFile.name}:\n  " + failures.join('\n  ')
            )
        }

        sentinelFile.parentFile.mkdirs()
        sentinelFile.text = "OK: ${new Date()}\n"
        logger.lifecycle("Doctests OK: ${readmeFile.name} (${blocks.size()} blocks)")
    }
}