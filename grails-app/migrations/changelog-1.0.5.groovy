databaseChangeLog = {

  changeSet(author: "Diogo Cabral de Almeida", id: "221120161144") {

    //unused columns
    dropColumn(tableName: "group", columnName: "hash")

    dropColumn(tableName: "problem", columnName: "fastest_submission_id")

    dropColumn(tableName: "profile", columnName: "problems_correct")
    dropColumn(tableName: "profile", columnName: "problems_tried")
    dropColumn(tableName: "profile", columnName: "submission_correct_count")
    dropColumn(tableName: "profile", columnName: "submission_count")
    dropColumn(tableName: "profile", columnName: "top_coder_position")
    dropColumn(tableName: "profile", columnName: "top_coder_score")
    
    dropColumn(tableName: "questionnaire", columnName: "update_status")

    dropColumn(tableName: "test_case", columnName: "rank")
    dropColumn(tableName: "test_case", columnName: "unrank")
    dropColumn(tableName: "test_case", columnName: "max_output_size")

    //fixing submission tries
    sql("""UPDATE submission s set tries = (
                select count(*) from submission where 1 = 1
                and user_id = s.user_id 
                and problem_id = s.problem_id
                and submission_date < s.submission_date
            ) + 1""")

    //fork bombs
    sql("""UPDATE submission set evaluation = 11
        WHERE id IN
        (
            101480, 101481, 89831, 89844, 89841, 89837, 89834, 101434, 101460, 101441,
            320501, 101440, 101463, 101469, 89827, 89826, 101454, 101452, 170603, 170601,
            88930, 101468, 101467, 101465, 101464, 101462, 101450, 101475, 101472, 101471,
            101470, 118975, 118974, 89344, 101443, 101474, 101436, 101435, 88929, 372869,
            320500
        )""")
  }

  changeSet(author: "Marcio Aguiar", id: "221120161308") {
    renameTable(oldTableName: "message", newTableName: "old_message")
    renameTable(oldTableName: "message_message", newTableName: "old_message_message")
    renameSequence(oldSequenceName: "message_id_seq", newSequenceName: "old_message_id_seq")

    dropTable(tableName: "message_information")

    sql("ALTER TABLE old_message_message DROP CONSTRAINT message_message_message_id_fkey")
    sql("ALTER TABLE old_message_message DROP CONSTRAINT message_message_message_responses_id_fkey")
    sql("ALTER TABLE old_message DROP CONSTRAINT message_group_id_fkey")
    sql("ALTER TABLE old_message DROP CONSTRAINT message_pkey")

    sql("DROP INDEX message_on_group_id_idx")
    sql("DROP INDEX message_on_recipient_id_idx")
    sql("DROP INDEX message_on_sender_id_idx")

    createTable(tableName: "message_group") {
      column(name: "id", type: "bigint") {
        constraints(nullable: "false", primaryKey: "true", primaryKeyName: "message_group_pkey")
      }

      column(name: "version", type: "bigint")
      column(name: "subject", type: "varchar(255)")
      column(name: "date_created", type: "timestamp without time zone")
      column(name: "last_updated", type: "timestamp without time zone")
      column(name: "type", type: "integer")
      column(name: "group_id", type: "bigint")
      column(name: "user_id", type: "bigint")
      column(name: "recipient_id", type: "bigint")
      column(name: "problem_id", type: "bigint")
      column(name: "message_status", type: "integer")
    }

    createIndex(indexName: "message_group_on_group_id_idx", tableName: "message_group") {
      column(name: "group_id")
    }

    createIndex(indexName: "message_group_on_date_created_idx", tableName: "message_group") {
      column(name: "date_created")
    }

    createIndex(indexName: "message_group_on_user_id_idx", tableName: "message_group") {
      column(name: "user_id")
    }

    createIndex(indexName: "message_group_on_problem_id_idx", tableName: "message_group") {
      column(name: "problem_id")
    }

    addForeignKeyConstraint(constraintName: "message_group_user_id_fkey", baseColumnNames: "user_id",
            baseTableName: "message_group", referencedColumnNames: "id", referencedTableName: "user")

    addForeignKeyConstraint(constraintName: "message_group_group_id_fkey", baseColumnNames: "group_id",
            baseTableName: "message_group", referencedColumnNames: "id", referencedTableName: "group")

    addForeignKeyConstraint(constraintName: "message_group_problem_id_fkey", baseColumnNames: "problem_id",
            baseTableName: "message_group", referencedColumnNames: "id", referencedTableName: "problem")

    addForeignKeyConstraint(constraintName: "message_group_recipient_id_fkey", baseColumnNames: "recipient_id",
            baseTableName: "message_group", referencedColumnNames: "id", referencedTableName: "user")

    createTable(tableName: "message") {
      column(name: "id", type: "bigint") {
        constraints(nullable: "false", primaryKey: "true", primaryKeyName: "message_pkey")
      }

      column(name: "version", type: "bigint")
      column(name: "body", type: "text")
      column(name: "date_created", type: "timestamp without time zone")
      column(name: "last_updated", type: "timestamp without time zone")
      column(name: "sender_id", type: "bigint")
      column(name: "message_group_id", type: "bigint")
    }

    createIndex(indexName: "message_on_date_created_idx", tableName: "message") {
      column(name: "date_created")
    }

    createIndex(indexName: "message_on_sender_id_idx", tableName: "message") {
      column(name: "sender_id")
    }

    addForeignKeyConstraint(constraintName: "message_sender_id_fkey", baseColumnNames: "sender_id",
            baseTableName: "message", referencedColumnNames: "id", referencedTableName: "user")

    addForeignKeyConstraint(constraintName: "message_message_group_id_fkey", baseColumnNames: "message_group_id",
            baseTableName: "message", referencedColumnNames: "id", referencedTableName: "message_group")

    createSequence(sequenceName: "message_id_seq")
    createSequence(sequenceName: "message_group_id_seq")
  }

  changeSet(author: "Marcio Aguiar", id: "301120161800") {
    createTable(tableName: "message_view_event") {
      column(name: "version", type: "bigint")
      column(name: "message_group_id", type: "bigint")
      column(name: "user_id", type: "bigint")
      column(name: "last_view_millis", type: "bigint")
    }

    addPrimaryKey(columnNames: "message_group_id, user_id", constraintName:"message_view_event_pkey", tableName: "message_view_event")

    addForeignKeyConstraint(constraintName: "message_view_event_msg_group_fkey", baseColumnNames: "message_group_id",
            baseTableName: "message_view_event", referencedColumnNames: "id", referencedTableName: "message_group")

    addForeignKeyConstraint(constraintName: "message_view_event_user_fkey", baseColumnNames: "user_id",
            baseTableName: "message_view_event", referencedColumnNames: "id", referencedTableName: "user")
  }
  
}