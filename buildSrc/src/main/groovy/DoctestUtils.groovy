class DoctestUtils {
    static final List<String> SUPPORTED_LANGS = ['java', 'kotlin']

    static String normalizeLineEndings(String text) {
        text.replace('\r\n', '\n').replace('\r', '\n')
    }

    static List<Map> extractCodeBlocks(File readme) {
        def blocks = []
        def lines = readme.readLines('UTF-8')
        boolean inBlock = false
        Map currentBlock = null
        int globalIdx = 0

        lines.eachWithIndex { String line, int lineIdx ->
            if (!inBlock) {
                // Match fences like ```java or ```java block01
                def m = line =~ /^```(\w+)(?:\s+(\S+))?\s*$/
                if (m.matches()) {
                    def lang = (m[0][1] as String).toLowerCase()
                    if (lang in SUPPORTED_LANGS) {
                        inBlock = true
                        globalIdx++
                        def explicitId = m[0][2] ? (m[0][2] as String) : null
                        def id = explicitId ?: "block${globalIdx.toString().padLeft(2, '0')}"
                        currentBlock = [lang: lang, startLine: lineIdx + 1, content: [], id: id, blockNum: globalIdx]
                    }
                }
            } else {
                if (line.trim() == '```') {
                    inBlock = false
                    blocks << currentBlock
                    currentBlock = null
                } else {
                    currentBlock.content << line
                }
            }
        }

        blocks
    }

    static String extractMarkerContent(File file, String marker) {
        def text = normalizeLineEndings(file.text)
        def beginTag = "// @DOCTEST-BEGIN ${marker}"
        def endTag = '// @DOCTEST-END'

        def beginIdx = text.indexOf(beginTag)
        if (beginIdx < 0) return null
        def afterBegin = beginIdx + beginTag.length()
        def endIdx = text.indexOf(endTag, afterBegin)
        if (endIdx < 0) return null

        text.substring(afterBegin, endIdx)
    }

    // Strip common leading whitespace from all non-empty lines so that
    // content indented inside a method body compares equal to the raw README block.
    static String dedent(String text) {
        def lines = normalizeLineEndings(text).split('\n')
        def nonEmpty = lines.findAll { it.trim() }
        if (!nonEmpty) return text.trim()
        def minIndent = nonEmpty.collect { line ->
            line.length() - line.stripLeading().length()
        }.min()
        lines.collect { it.length() >= minIndent ? it.substring(minIndent) : it.trim() }.join('\n').trim()
    }

    static String classNameFor(String lang, String id) {
        "README_${lang}_${id}_Test"
    }

    static String fileExtFor(String lang) {
        lang == 'kotlin' ? 'kt' : 'java'
    }
}