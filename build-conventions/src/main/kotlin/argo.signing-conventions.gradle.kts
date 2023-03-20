import java.util.*

plugins {
    signing
}

if (System.getenv("CI") != null) {
    signing {
        val signingKeyId: String? by project
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKeyId, base64Decode(signingKey), signingPassword)
    }
}

fun base64Decode(secret: String?) =
    secret?.let {
        String(Base64.getDecoder().decode(secret))
    }
