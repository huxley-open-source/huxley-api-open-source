databaseChangeLog = {

    changeSet(author: "Marcio Aguiar", id: "100520171045") {

        createTable(tableName: "questionnaire_view_event") {
            column(name: "id", type: "bigint") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "questionnaire_view_event_pkey")
            }

            column(name: "version", type: "bigint")
            column(name: "quiz_id", type: "bigint")
            column(name: "user_id", type: "bigint")
            column(name: "view_date", type: "timestamp without time zone")

        }

        addForeignKeyConstraint(constraintName: "quiz_view_event_quiz_id_fkey", baseColumnNames: "quiz_id", baseTableName: "questionnaire_view_event", referencedColumnNames: "id", referencedTableName: "questionnaire")
        addForeignKeyConstraint(constraintName: "quiz_view_event_user_id_fkey", baseColumnNames: "user_id", baseTableName: "questionnaire_view_event", referencedColumnNames: "id", referencedTableName: "user")

        createSequence(sequenceName: "questionnaire_view_event_seq")

    }

    changeSet(author: "Marcio Aguiar", id: "160520170949") {
        dropForeignKeyConstraint(constraintName: "quiz_view_event_quiz_id_fkey", baseTableName: "questionnaire_view_event")
        dropForeignKeyConstraint(constraintName: "quiz_view_event_user_id_fkey", baseTableName: "questionnaire_view_event")
    }
}