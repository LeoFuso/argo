object Versions {

    const val ARGO: String = "0.1.2-SNAPSHOT"
    const val DETEKT: String = "1.22.0"
    const val COMPILER: String = "1.11.1"

    fun isSnapshot() = ARGO.endsWith("SNAPSHOT")
}
