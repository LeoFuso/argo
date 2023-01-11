package io.github.leofuso.argo.kotlin.run

import io.github.leofuso.obs.demo.events.Department
import io.github.leofuso.obs.demo.events.Details

fun main() {
    val builder = Details.newBuilder()
    builder.annotation = "Some annotation"
    builder.department = Department.ALLOWANCE
    val details = builder.build()
}
