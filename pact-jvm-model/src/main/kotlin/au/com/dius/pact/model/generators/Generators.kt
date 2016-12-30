package au.com.dius.pact.model.generators

import java.util.*

enum class Category {
    METHOD, PATH, HEADER, QUERY, BODY, STATUS
}

interface Generators {
}

data class ExampleGenerators(val categories: Map<Category, Any> = HashMap()) : Generators {
}
