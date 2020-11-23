databaseChangeLog = {

    changeSet(author: "Marcio Aguiar", id: "131220160904") {
        createTable(tableName: "notification_preferences") {
            column(name: "version", type: "bigint")
            column(name: "user_id", type: "bigint") {
                constraints(nullable: "false")
            }
            column(name: "notification_type", type: "integer") {
                constraints(nullable: "false")
            }

            column(name: "email", type: "boolean")
            column(name: "web", type: "boolean")
            column(name: "digested", type: "boolean")
        }

        addPrimaryKey(columnNames: "user_id, notification_type", constraintName: "notification_preferences_pkey", tableName: "notification_preferences")
        addForeignKeyConstraint(constraintName: "notification_preferences_user_fkey", baseColumnNames: "user_id",
                baseTableName: "notification_preferences", referencedColumnNames: "id", referencedTableName: "user")
    }

    changeSet(author: "Diogo Cabral de Almeida", id: "131220161037") {
        addColumn(tableName: "submission") {
            column(name: "content_hash", type: "character varying(255)")
        }
    }

}