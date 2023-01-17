tasks.register("clean") {
    group = "build"
    description = "Delegates task-action to all included builds."
    val paths = subprojects.map { ":${it.name}:clean" }.toTypedArray()
    dependsOn(paths)
}
