databaseChangeLog = {

    changeSet(author: "Marcio Aguiar", id: "010820171302") {
        createTable(tableName: "submission_restrictions_evaluation") {
            column(name: "submission_id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "submission_restrictions_evaluation_pkey")
            }
            column(name: "version", type: "bigint")
            column(name: "user_id", type: "bigint")
            column(name: "problem_id", type: "bigint")
            column(name: "quiz_id", type: "bigint")
            column(name: "restriction_count", type: "integer")
            column(name: "wrong_restriction_count", type: "integer")
            column(name: "result", type: "text")
        }

    }

}