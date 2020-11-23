package com.thehuxley

class Plagiarism implements Serializable {

    enum Status {
        WAITING, CONFIRMED, DISCARDED
    }

    Submission submission1
    Submission submission2
    double percentage
    Status status = Status.WAITING

    static mapping = {
        table "plagiarism"
        id generator: "sequence", params: [sequence: "plagiarism_id_seq"]
        status enumType: "ordinal"
    }

}
