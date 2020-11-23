package com.thehuxley

class Language implements Serializable {

    String name
    String execParams
    String compileParams
    String compiler
    String script
    String extension
    String label

    static searchable = {
        only = ["name"]
    }

    static mapping = {
        id generator: "sequence", params: [sequence: "language_id_seq"]
    }

    static constraints = {
        name blank: false, unique: true
        compileParams blank: true, nullable: true
        compiler black: false
        execParams blank: true, nullable: true
        label blank: false, nullable: false
    }

}
