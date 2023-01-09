package io.github.leofuso.argo.plugin.tasks

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File


const val MINIMUM_NUMBER_OF_CHARACTERS: Int = 3
const val FIRST_COMES_FIRST: Int = -1
const val SECOND_COMES_FIRST: Int = 1

class SchemaResolverBySorting(private val mapper: ObjectMapper) : Comparator<File> {

    override fun compare(o1: File, o2: File): Int {

        val firstSchema = mapper.readTree(o1)
        val namespaceValue = firstSchema.path("namespace")
        val firstNamespace = namespaceValue.asText()

        val nameValue = firstSchema.path("name")
        val firstName = nameValue.asText()

        val possibleReference = "$firstNamespace.$firstName"
        if (possibleReference.length < MINIMUM_NUMBER_OF_CHARACTERS) {
            return FIRST_COMES_FIRST
        }

        val secondSchema = mapper.readTree(o2)
        val found = secondSchema.find { node -> node.isTextual && node.asText() == possibleReference }
        if (found != null) {
            return FIRST_COMES_FIRST
        }
        return SECOND_COMES_FIRST
    }
}
