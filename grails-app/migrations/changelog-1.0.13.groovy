databaseChangeLog = {

    changeSet(author: "Marcio Aguiar", id: "230320171133") {

        sql("""
            UPDATE submission as s SET
                    total_test_cases = sub.total,
                    correct_test_cases = sub.correct,
                    time = sub.worst_time,
                    evaluation = sub.eval
                FROM (
                  SELECT id,
                        (test_count - correct_examples)                   AS total,
                        ((test_count - correct_examples) - wrong)         AS correct,
                        (CASE WHEN wrong = 0 THEN 0 ELSE worst_eval END)  AS eval,
                        worst_time                                        AS worst_time
                    FROM (
                      SELECT
                        s.id AS id,
                        COUNT(tc) AS test_count,
                        min(CASE WHEN tc.evaluation = 0 THEN 20 ELSE tc.evaluation END) AS worst_eval,
                        max(tc.time) AS worst_time,
                        sum(CASE WHEN tc.evaluation = 0 AND t.example is true THEN 1 ELSE 0 END) AS correct_examples,
                        sum(CASE WHEN tc.evaluation != 0 THEN 1 ELSE 0 END) AS wrong
                      FROM submission s
                      JOIN test_case_evaluation tc on tc.submission_id = s.id
                      JOIN test_case t on tc.test_case_id = t.id
                      GROUP BY s.id) AS s) AS sub
                WHERE s.id = sub.id""")
    }

    changeSet(author: "Marcio Aguiar", id: "280320171046") {

        sql('''insert into notification_preferences (
                    select
                        1, id, 2, true, false, false
                    from public.user
                    where id not in (
                        select user_id from notification_preferences where notification_type = 2
                    )
                )''')

    }

}