databaseChangeLog = {

    changeSet(author: "Marcio Aguiar", id: "170720171645") {
        createTable(tableName: "user_problem_vote") {
            column(name: "version", type: "bigint")
            column(name: "user_id", type: "bigint") {
                constraints(nullable: "false")
            }
            column(name: "problem_id", type: "bigint") {
                constraints(nullable: "false")
            }
            column(name: "score", type: "smallint") {
                constraints(nullable: "false")
            }
        }
        addPrimaryKey(columnNames: "user_id, problem_id", constraintName:"user_problem_vote_pkey", tableName: "user_problem_vote")
        addForeignKeyConstraint(constraintName: "user_problem_vote_problem_fkey", baseColumnNames: "problem_id",
                baseTableName: "user_problem_vote", referencedColumnNames: "id", referencedTableName: "problem")

        addForeignKeyConstraint(constraintName: "user_problem_vote_user_fkey", baseColumnNames: "user_id",
                baseTableName: "user_problem_vote", referencedColumnNames: "id", referencedTableName: "user")
    }

    changeSet(author: "Marcio Aguiar", id: "180720171050") {
        addColumn(tableName: "problem") {
            column(name: "user_rank", type: "real")
        }

        addColumn(tableName: "problem") {
            column(name: "rank_votes", type: "integer")
        }
    }

    changeSet(author: "Marcio Aguiar", id: "260720171145") {
        sql("ALTER TABLE problem ALTER COLUMN rank_votes SET DEFAULT 0")
        sql("ALTER TABLE problem ALTER COLUMN user_rank SET DEFAULT 0")
        sql("UPDATE problem SET user_rank = 0")
        sql("UPDATE problem SET rank_votes = 0")
    }

}