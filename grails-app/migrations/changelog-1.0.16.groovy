databaseChangeLog = {

    changeSet(author: "Marcio Aguiar", id: "140620171211") {

        createTable(tableName: "quiz_problem_restriction") {
            column(name: "version", type: "bigint")
            column(name: "quiz_id", type: "bigint")
            column(name: "problem_id", type: "bigint")
            column(name: "restrictions", type: "varchar(1500)")
            column(name: "penalty", type: "real")
        }

        addPrimaryKey(columnNames: "quiz_id, problem_id", constraintName:"quiz_problem_restriction_pkey", tableName: "quiz_problem_restriction")

    }
}