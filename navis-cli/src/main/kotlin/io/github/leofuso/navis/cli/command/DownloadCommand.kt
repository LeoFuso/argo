package io.github.leofuso.navis.cli.command

import com.github.ajalt.clikt.core.CliktCommand

class DownloadCommand  : CliktCommand(
    name = "download",
    help = """
    Download Schemas from Registry.
    
    Accepts Subject with and without a version or a Pattern as search parameters.
    
    Download 'Schema'(s) to 'DEST'.
    
    """,
    printHelpOnEmptyArgs = true
) {
    override fun run() {
        TODO("Not yet implemented")
    }
}
