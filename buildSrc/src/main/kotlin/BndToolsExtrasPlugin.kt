
data class Requires(
    val module: String,
    val static: Boolean = false,
    val transitive: Boolean = false,
)

fun bndFile(moduleName: String, requiredModules: List<Requires>): String {
    return """
        Bundle-SymbolicName: $moduleName

        Import-Package: ${
            requiredModules
                .filter { it.static }
                .map { it.module }
                .joinToString(",") { "!${it},!{${it}}.*" }
        }, *
        -jpms-module-info: $moduleName;\
                  access="SYNTHETIC,MANDATED";\
                  modules="${
                      requiredModules.joinToString(",") { it.module }
                  }"
        -jpms-module-info-options: ${
            requiredModules.joinToString(",") {
                listOfNotNull(
                    it.module,
                    if (it.static) "static=\"${it.static}\"" else null,
                    if (it.transitive) "transitive=\"${it.transitive}\"" else null
                ).joinToString(";")
            }
        }
        -removeheaders: Private-Package,Tool
    """.trimIndent()
}

