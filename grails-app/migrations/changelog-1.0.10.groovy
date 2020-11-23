databaseChangeLog = {

    changeSet(author: "Diogo Cabral de Almeida", id: "020220171539") {

        sql('''insert into notification_preferences (
                    select 
                        1, id, 0, false, false, false 
                    from public.user 
                    where id not in (
                        select user_id from notification_preferences where notification_type = 0
                    )
                )''')

        sql('''insert into notification_preferences (
                    select 
                        1, id, 1, false, false, false 
                    from public.user 
                    where id not in (
                        select user_id from notification_preferences where notification_type = 1
                    )
                )''')

    }

    changeSet(author: "Marcio Aguiar Ribeiro", id: "030220171350") {

        sql('''insert into role (version, authority) values (1, 'ROLE_TRANSLATOR')''')
    }

    changeSet(author: "Marcio Aguiar Ribeiro", id: "130220171448") {
        sql('''update submission set
                total_test_cases = subq.total_test_cases,
                correct_test_cases = subq.correct_test_cases FROM
                    (select submission_id,
                            count(*) as total_test_cases,
                            sum(case when evaluation = 0 then 1 else 0 end) as correct_test_cases
                            from test_case_evaluation group by submission_id) as subq
                    WHERE submission.id = subq.submission_id''')
    }
}