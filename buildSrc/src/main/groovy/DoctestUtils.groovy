class DoctestUtils {
    static final List<String> SUPPORTED_LANGS = ['java', 'kotlin']

    static List<Map> extractCodeBlocks(File readme) {
        def blocks = []
        def lines = readme.readLines('UTF-8')
        boolean inBlock = false
        Map currentBlock = null

        lines.eachWithIndex { String line, int lineIdx ->
            if (!inBlock) {
                def m = line =~ /^```(\w+)\s*$/
                if (m.matches()) {
                    def lang = (m[0][1] as String).toLowerCase()
                    if (lang in SUPPORTED_LANGS) {
                        inBlock = true
                        currentBlock = [lang: lang, startLine: lineIdx + 1, content: []]
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
        def text = file.text
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
        def lines = text.split('\n')
        def nonEmpty = lines.findAll { it.trim() }
        if (!nonEmpty) return text.trim()
        def minIndent = nonEmpty.collect { line ->
            line.length() - line.stripLeading().length()
        }.min()
        lines.collect { it.length() >= minIndent ? it.substring(minIndent) : it.trim() }.join('\n').trim()
    }

    static String classNameFor(String lang, int blockNum) {
        "README_${lang}_block${blockNum.toString().padLeft(2, '0')}_Test"
    }

    static String fileExtFor(String lang) {
        lang == 'kotlin' ? 'kt' : 'java'
    }
}
