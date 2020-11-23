databaseChangeLog = {

	changeSet(author: "Diogo Cabral de Almeida", id: "211020161550") {

		createTable(tableName: "cluster") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "cluster_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "name", type: "character varying(255)")
			column(name: "institution_id", type: "bigint")
			column(name: "hash", type: "character varying(255)")
			column(name: "date_created", type: "timestamp without time zone")
			column(name: "last_updated", type: "timestamp without time zone")
			column(name: "description", type: "character varying(255)")
			column(name: "access_key", type: "character varying(255)")
			column(name: "end_date", type: "timestamp without time zone")
			column(name: "start_date", type: "timestamp without time zone")
			column(name: "url", type: "character varying(255)")
		}

		createTable(tableName: "feed") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "feed_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "date_created", type: "timestamp without time zone")
			column(name: "last_updated", type: "timestamp without time zone")
			column(name: "type", type: "bigint")
			column(name: "recipient_id", type: "bigint")
		}

		createTable(tableName: "feed_body") {
			column(name: "body", type: "bigint")
			column(name: "body_idx", type: "character varying(255)")
			column(name: "body_elt", type: "character varying(255)")
			column(name: "feed_id", type: "bigint")
			column(name: "body_object", type: "character varying(255)")
		}

		createTable(tableName: "institution") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "institution_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "name", type: "character varying(255)")
			column(name: "phone", type: "character varying(255)")
			column(name: "logo", type: "character varying(255)")
			column(name: "status", type: "bigint")
			column(name: "acronym", type: "character varying(20)")
		}

		createTable(tableName: "language") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "language_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "exec_params", type: "character varying(255)")
			column(name: "name", type: "character varying(255)")
			column(name: "compile_params", type: "character varying(255)")
			column(name: "compiler", type: "character varying(255)")
			column(name: "script", type: "character varying(255)")
			column(name: "extension", type: "character varying(255)")
			column(name: "label", type: "character varying(255)")
		}

		createTable(tableName: "license") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "license_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "date_created", type: "timestamp without time zone")
			column(name: "end_date", type: "timestamp without time zone")
			column(name: "hash", type: "character varying(255)")
			column(name: "institution_id", type: "bigint")
			column(name: "last_updated", type: "timestamp without time zone")
			column(name: "start_date", type: "timestamp without time zone")
			column(name: "type_id", type: "bigint")
			column(name: "user_id", type: "bigint")
			column(name: "active", type: "boolean")
			column(name: "indefinite_validity", type: "boolean")
		}

		createTable(tableName: "license_pack") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "license_pack_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "date_created", type: "timestamp without time zone")
			column(name: "end_date", type: "timestamp without time zone")
			column(name: "institution_id", type: "bigint")
			column(name: "last_updated", type: "timestamp without time zone")
			column(name: "start_date", type: "timestamp without time zone")
			column(name: "total", type: "bigint")
		}

		createTable(tableName: "license_type") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "license_type_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "date_created", type: "timestamp without time zone")
			column(name: "description", type: "character varying(255)")
			column(name: "descriptor", type: "character varying(255)")
			column(name: "kind", type: "character varying(17)")
			column(name: "last_updated", type: "timestamp without time zone")
			column(name: "name", type: "character varying(255)")
		}

		createTable(tableName: "message") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "message_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "subject", type: "character varying(255)")
			column(name: "body", type: "text")
			column(name: "date_created", type: "timestamp without time zone")
			column(name: "last_updated", type: "timestamp without time zone")
			column(name: "read_date", type: "timestamp without time zone")
			column(name: "group_id", type: "bigint")
			column(name: "sender_id", type: "bigint")
			column(name: "recipient_id", type: "bigint")
			column(name: "type", type: "bigint")
			column(name: "unread", type: "boolean")
			column(name: "deleted", type: "boolean")
			column(name: "first_message", type: "boolean")
		}

		createTable(tableName: "message_information") {
			column(name: "message_id", type: "bigint")
			column(name: "information", type: "bigint")
			column(name: "information_idx", type: "character varying(255)")
			column(name: "information_elt", type: "character varying(255)")
			column(name: "information_string", type: "character varying(255)")
		}

		createTable(tableName: "message_message") {
			column(name: "message_responses_id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "message_message_pkey")
			}
			column(name: "message_id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "message_message_pkey")
			}
		}

		createTable(tableName: "pendency") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "pendency_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "date_created", type: "timestamp without time zone")
			column(name: "last_updated", type: "timestamp without time zone")
			column(name: "status", type: "bigint")
			column(name: "group_id", type: "bigint")
			column(name: "institution_id", type: "bigint")
			column(name: "user_id", type: "bigint")
			column(name: "kind", type: "bigint")
		}

		createTable(tableName: "pendency_key") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "pendency_key_pkey")
			}
			column(name: "hash_key", type: "character varying(255)")
			column(name: "type", type: "bigint")
			column(name: "entity", type: "bigint")
			column(name: "date_created", type: "timestamp without time zone")
			column(name: "last_updated", type: "timestamp without time zone")
		}

		createTable(tableName: "pendency_params") {
			column(name: "pendency_id", type: "bigint")
			column(name: "params", type: "bigint")
			column(name: "params_idx", type: "character varying(255)")
			column(name: "params_elt", type: "character varying(255)")
			column(name: "params_object", type: "character varying(255)")
		}

		createTable(tableName: "plagiarism") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "plagiarism_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "percentage", type: "double precision")
			column(name: "submission1_id", type: "bigint")
			column(name: "submission2_id", type: "bigint")
			column(name: "status", type: "bigint DEFAULT 0")
		}

		createTable(tableName: "plagiarism_status") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "plagiarism_status_pkey")
			}
			column(name: "name", type: "character varying(255)")
		}

		createTable(tableName: "problem") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "problem_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "time_limit", type: "bigint")
			column(name: "level", type: "bigint")
			column(name: "nd", type: "double precision")
			column(name: "description", type: "text")
			column(name: "name", type: "character varying(255)")
			column(name: "status", type: "bigint")
			column(name: "user_approved_id", type: "bigint")
			column(name: "user_suggest_id", type: "bigint")
			column(name: "fastest_submission_id", type: "bigint")
			column(name: "input_format", type: "text")
			column(name: "output_format", type: "text")
			column(name: "date_created", type: "timestamp without time zone")
			column(name: "last_updated", type: "timestamp without time zone")
			column(name: "source", type: "character varying(255)")
			column(name: "last_user_update", type: "timestamp without time zone")			
		}

		createTable(tableName: "profile") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "profile_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "hash", type: "character varying(255)")
			column(name: "name", type: "character varying(255)")
			column(name: "photo", type: "character varying(255)")
			column(name: "problems_correct", type: "bigint")
			column(name: "problems_tried", type: "bigint")
			column(name: "small_photo", type: "character varying(255)")
			column(name: "user_id", type: "bigint")
			column(name: "date_created", type: "timestamp without time zone")
			column(name: "last_updated", type: "timestamp without time zone")
			column(name: "institution_id", type: "bigint")
			column(name: "submission_correct_count", type: "bigint")
			column(name: "submission_count", type: "bigint")
			column(name: "top_coder_score", type: "double precision")
			column(name: "top_coder_position", type: "bigint")
			column(name: "last_login", type: "timestamp without time zone")
		}

		createTable(tableName: "questionnaire") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "questionnaire_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "start_date", type: "timestamp without time zone")
			column(name: "score", type: "double precision")
			column(name: "end_date", type: "timestamp without time zone")
			column(name: "title", type: "character varying(255)")
			column(name: "description", type: "text")
			column(name: "date_created", type: "timestamp without time zone")
			column(name: "last_updated", type: "timestamp without time zone")
			column(name: "group_id", type: "bigint")
			column(name: "update_status", type: "bigint")
		}

		createTable(tableName: "questionnaire_problem") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "questionnaire_problem_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "questionnaire_id", type: "bigint")
			column(name: "score", type: "double precision")
			column(name: "problem_id", type: "bigint")
		}

		createTable(tableName: "questionnaire_user") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "questionnaire_user_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "questionnaire_id", type: "bigint")
			column(name: "score", type: "double precision")
			column(name: "user_id", type: "bigint")
			column(name: "comment", type: "text")
			column(name: "status", type: "bigint")
			column(name: "submissions_count", type: "bigint")
			column(name: "problems_tried", type: "bigint")
			column(name: "problems_correct", type: "bigint")
		}

		createTable(tableName: "questionnaire_user_penalty") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "questionnaire_user_penalty_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "date_created", type: "timestamp without time zone")
			column(name: "last_updated", type: "timestamp without time zone")
			column(name: "penalty", type: "double precision")
			column(name: "questionnaire_problem_id", type: "bigint")
			column(name: "questionnaire_user_id", type: "bigint")
		}

		createTable(tableName: "role") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "role_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "authority", type: "character varying(255)")
		}

		createTable(tableName: "submission") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "submission_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "problem_id", type: "bigint")
			column(name: "submission", type: "character varying(255)")
			column(name: "evaluation", type: "bigint")
			column(name: "submission_date", type: "timestamp without time zone")
			column(name: "diff_file", type: "text")
			column(name: "language_id", type: "bigint")
			column(name: "tries", type: "bigint")
			column(name: "output", type: "text")
			column(name: "user_id", type: "bigint")
			column(name: "time", type: "double precision DEFAULT '-1'::bigint")
			column(name: "plagiarism_status_id", type: "bigint DEFAULT 1")
			column(name: "error_msg", type: "text")
			column(name: "test_case_id", type: "bigint")
			column(name: "comment", type: "text")
		}

		createTable(tableName: "test_case") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "test_case_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "input", type: "text")
			column(name: "output", type: "text")
			column(name: "problem_id", type: "bigint")
			column(name: "max_output_size", type: "double precision")
			column(name: "tip", type: "text")
			column(name: "rank", type: "bigint")
			column(name: "unrank", type: "bigint")
			column(name: "date_created", type: "timestamp without time zone")
			column(name: "last_updated", type: "timestamp without time zone")
			column(name: "example", type: "boolean")
		}

		createTable(tableName: "top_coder") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "top_coder_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "points", type: "double precision")
			column(name: "user_id", type: "bigint")
			column(name: "position", type: "bigint")
		}

		createTable(tableName: "topic") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "topic_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "name", type: "character varying(255)")
		}

		createTable(tableName: "topic_problems") {
			column(name: "topic_id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "topic_problems_pkey")
			}
			column(name: "problem_id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "topic_problems_pkey")
			}
		}

		createTable(tableName: "user") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "user_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "name", type: "character varying(255)")
			column(name: "email", type: "character varying(255)")
			column(name: "username", type: "character varying(255)")
			column(name: "password", type: "character varying(255)")
			column(name: "date_created", type: "timestamp without time zone")
			column(name: "last_updated", type: "timestamp without time zone")
			column(name: "avatar", type: "character varying(255)")
			column(name: "institution_id", type: "bigint")
			column(name: "account_expired", type: "boolean")
			column(name: "account_locked", type: "boolean")
			column(name: "enabled", type: "boolean")
			column(name: "password_expired", type: "boolean")
		}

		createTable(tableName: "user_cluster") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "user_cluster_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "user_id", type: "bigint")
			column(name: "cluster_id", type: "bigint")
			column(name: "role", type: "bigint")
			column(name: "enabled", type: "boolean")
		}

		createTable(tableName: "user_institution") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "user_institution_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "user_id", type: "bigint")
			column(name: "institution_id", type: "bigint")
			column(name: "role", type: "bigint")
			column(name: "enabled", type: "boolean")
		}

		createTable(tableName: "user_problem") {
			column(name: "id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "user_problem_pkey")
			}
			column(name: "version", type: "bigint")
			column(name: "problem_id", type: "bigint")
			column(name: "status", type: "bigint")
			column(name: "user_id", type: "bigint")
			column(name: "similarity", type: "bigint")
		}

		createTable(tableName: "user_role") {
			column(name: "user_id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "user_role_pkey")
			}
			column(name: "role_id", type: "bigint") {
			    constraints(nullable: "false", primaryKey: "true", primaryKeyName: "user_role_pkey")
			}
		}

		createSequence(sequenceName: "cluster_id_seq")
		createSequence(sequenceName: "feed_id_seq")
		createSequence(sequenceName: "institution_id_seq")
		createSequence(sequenceName: "language_id_seq")
		createSequence(sequenceName: "license_id_seq")
		createSequence(sequenceName: "license_pack_id_seq")
		createSequence(sequenceName: "license_type_id_seq")
		createSequence(sequenceName: "message_id_seq")
		createSequence(sequenceName: "pendency_id_seq")
		createSequence(sequenceName: "pendency_key_id_seq")
		createSequence(sequenceName: "plagiarism_id_seq")
		createSequence(sequenceName: "plagiarism_status_id_seq")
		createSequence(sequenceName: "problem_id_seq")
		createSequence(sequenceName: "profile_id_seq")
		createSequence(sequenceName: "questionnaire_id_seq")
		createSequence(sequenceName: "questionnaire_problem_id_seq")
		createSequence(sequenceName: "questionnaire_user_id_seq")
		createSequence(sequenceName: "questionnaire_user_penalty_id_seq")
		createSequence(sequenceName: "role_id_seq")
		createSequence(sequenceName: "submission_id_seq")
		createSequence(sequenceName: "test_case_id_seq")
		createSequence(sequenceName: "top_coder_id_seq")
		createSequence(sequenceName: "topic_id_seq")
		createSequence(sequenceName: "user_cluster_id_seq")
		createSequence(sequenceName: "user_id_seq")
		createSequence(sequenceName: "user_institution_id_seq")
		createSequence(sequenceName: "user_problem_id_seq")

		addDefaultValue(tableName: "cluster", columnName: "id", defaultValueSequenceNext: "cluster_id_seq")
		addDefaultValue(tableName: "feed", columnName: "id", defaultValueSequenceNext: "feed_id_seq")
		addDefaultValue(tableName: "institution", columnName: "id", defaultValueSequenceNext: "institution_id_seq")
		addDefaultValue(tableName: "language", columnName: "id", defaultValueSequenceNext: "language_id_seq")
		addDefaultValue(tableName: "license", columnName: "id", defaultValueSequenceNext: "license_id_seq")
		addDefaultValue(tableName: "license_pack", columnName: "id", defaultValueSequenceNext: "license_pack_id_seq")
		addDefaultValue(tableName: "license_type", columnName: "id", defaultValueSequenceNext: "license_type_id_seq")
		addDefaultValue(tableName: "message", columnName: "id", defaultValueSequenceNext: "message_id_seq")
		addDefaultValue(tableName: "pendency", columnName: "id", defaultValueSequenceNext: "pendency_id_seq")
		addDefaultValue(tableName: "pendency_key", columnName: "id", defaultValueSequenceNext: "pendency_key_id_seq")
		addDefaultValue(tableName: "plagiarism", columnName: "id", defaultValueSequenceNext: "plagiarism_id_seq")
		addDefaultValue(tableName: "plagiarism_status", columnName: "id", defaultValueSequenceNext: "plagiarism_status_id_seq")
		addDefaultValue(tableName: "problem", columnName: "id", defaultValueSequenceNext: "problem_id_seq")
		addDefaultValue(tableName: "profile", columnName: "id", defaultValueSequenceNext: "profile_id_seq")
		addDefaultValue(tableName: "questionnaire", columnName: "id", defaultValueSequenceNext: "questionnaire_id_seq")	
		addDefaultValue(tableName: "questionnaire_problem", columnName: "id", defaultValueSequenceNext: "questionnaire_problem_id_seq")	
		addDefaultValue(tableName: "questionnaire_user", columnName: "id", defaultValueSequenceNext: "questionnaire_user_id_seq")	
		addDefaultValue(tableName: "questionnaire_user_penalty", columnName: "id", defaultValueSequenceNext: "questionnaire_user_penalty_id_seq")
		addDefaultValue(tableName: "role", columnName: "id", defaultValueSequenceNext: "role_id_seq")
		addDefaultValue(tableName: "submission", columnName: "id", defaultValueSequenceNext: "submission_id_seq")
		addDefaultValue(tableName: "test_case", columnName: "id", defaultValueSequenceNext: "test_case_id_seq")
		addDefaultValue(tableName: "top_coder", columnName: "id", defaultValueSequenceNext: "top_coder_id_seq")
		addDefaultValue(tableName: "topic", columnName: "id", defaultValueSequenceNext: "topic_id_seq")
		addDefaultValue(tableName: "user", columnName: "id", defaultValueSequenceNext: "user_id_seq")
		addDefaultValue(tableName: "user_cluster", columnName: "id", defaultValueSequenceNext: "user_cluster_id_seq")	
		addDefaultValue(tableName: "user_institution", columnName: "id", defaultValueSequenceNext: "user_institution_id_seq")		
		addDefaultValue(tableName: "user_problem", columnName: "id", defaultValueSequenceNext: "user_problem_id_seq")

		createIndex(indexName: "cluster_on_institution_id_idx", tableName: "cluster") {
			column(name: "institution_id")
		}

		createIndex(indexName: "feed_on_recipient_id_idx", tableName: "feed") {
			column(name: "recipient_id")
		}

		createIndex(indexName: "feed_body_on_feed_id_idx", tableName: "feed_body") {
			column(name: "feed_id")
		}

		createIndex(indexName: "license_on_institution_id_idx", tableName: "license") {
			column(name: "institution_id")
		}

		createIndex(indexName: "license_on_type_id_idx", tableName: "license") {
			column(name: "type_id")
		}

		createIndex(indexName: "license_on_user_id_idx", tableName: "license") {
			column(name: "user_id")
		}

		createIndex(indexName: "license_pack_on_institution_id_idx", tableName: "license_pack") {
			column(name: "institution_id")
		}

		createIndex(indexName: "message_on_group_id_idx", tableName: "message") {
			column(name: "group_id")
		}

		createIndex(indexName: "message_on_recipient_id_idx", tableName: "message") {
			column(name: "recipient_id")
		}

		createIndex(indexName: "message_on_sender_id_idx", tableName: "message") {
			column(name: "sender_id")
		}

		createIndex(indexName: "message_information_on_message_id_idx", tableName: "message_information") {
			column(name: "message_id")
		}

		createIndex(indexName: "message_message_on_message_id_idx", tableName: "message_message") {
			column(name: "message_id")
		}

		createIndex(indexName: "message_message_on_message_responses_id_idx", tableName: "message_message") {
			column(name: "message_responses_id")
		}

		createIndex(indexName: "pendency_on_group_id_idx", tableName: "pendency") {
			column(name: "group_id")
		}

		createIndex(indexName: "pendency_on_institution_id_idx", tableName: "pendency") {
			column(name: "institution_id")
		}

		createIndex(indexName: "pendency_on_user_id_idx", tableName: "pendency") {
			column(name: "user_id")
		}

		createIndex(indexName: "pendency_params_on_pendency_id_idx", tableName: "pendency_params") {
			column(name: "pendency_id")
		}

		createIndex(indexName: "plagiarism_on_submission1_id_idx", tableName: "plagiarism") {
			column(name: "submission1_id")
		}

		createIndex(indexName: "plagiarism_on_submission2_id_idx", tableName: "plagiarism") {
			column(name: "submission2_id")
		}

		createIndex(indexName: "problem_on_fastest_submission_id_idx", tableName: "problem") {
			column(name: "fastest_submission_id")
		}

		createIndex(indexName: "problem_on_user_approved_id_idx", tableName: "problem") {
			column(name: "user_approved_id")
		}

		createIndex(indexName: "problem_on_user_suggest_id_idx", tableName: "problem") {
			column(name: "user_suggest_id")
		}

		createIndex(indexName: "profile_on_institution_id_idx", tableName: "profile") {
			column(name: "institution_id")
		}

		createIndex(indexName: "profile_on_user_id_idx", tableName: "profile") {
			column(name: "user_id")
		}

		createIndex(indexName: "questionnaire_on_group_id_idx", tableName: "questionnaire") {
			column(name: "group_id")
		}

		createIndex(indexName: "questionnaire_problem_on_problem_id_idx", tableName: "questionnaire_problem") {
			column(name: "problem_id")
		}

		createIndex(indexName: "questionnaire_problem_on_questionnaire_id_idx", tableName: "questionnaire_problem") {
			column(name: "questionnaire_id")
		}

		createIndex(indexName: "questionnaire_user_on_questionnaire_id_idx", tableName: "questionnaire_user") {
			column(name: "questionnaire_id")
		}

		createIndex(indexName: "questionnaire_user_on_user_id_idx", tableName: "questionnaire_user") {
			column(name: "user_id")
		}

		createIndex(indexName: "questionnaire_user_penalty_on_questionnaire_problem_id_idx", tableName: "questionnaire_user_penalty") {
			column(name: "questionnaire_problem_id")
		}

		createIndex(indexName: "questionnaire_user_penalty_on_questionnaire_user_id_idx", tableName: "questionnaire_user_penalty") {
			column(name: "questionnaire_user_id")
		}

		createIndex(indexName: "submission_on_language_id_idx", tableName: "submission") {
			column(name: "language_id")
		}

		createIndex(indexName: "submission_on_problem_id_idx", tableName: "submission") {
			column(name: "problem_id")
		}

		createIndex(indexName: "submission_on_test_case_id_idx", tableName: "submission") {
			column(name: "test_case_id")
		}

		createIndex(indexName: "submission_on_user_id_idx", tableName: "submission") {
			column(name: "user_id")
		}

		createIndex(indexName: "submission_on_plagiarism_status_id_idx", tableName: "submission") {
			column(name: "plagiarism_status_id")
		}

		createIndex(indexName: "test_case_on_problem_id_idx", tableName: "test_case") {
			column(name: "problem_id")
		}

		createIndex(indexName: "top_coder_on_user_id_idx", tableName: "top_coder") {
			column(name: "user_id")
		}

		createIndex(indexName: "topic_problems_on_problem_id_idx", tableName: "topic_problems") {
			column(name: "problem_id")
		}

		createIndex(indexName: "topic_problems_on_topic_id_idx", tableName: "topic_problems") {
			column(name: "topic_id")
		}

		createIndex(indexName: "user_on_institution_id_idx", tableName: "user") {
			column(name: "institution_id")
		}

		createIndex(indexName: "user_cluster_on_cluster_id_idx", tableName: "user_cluster") {
			column(name: "cluster_id")
		}

		createIndex(indexName: "user_cluster_on_user_id_idx", tableName: "user_cluster") {
			column(name: "user_id")
		}

		createIndex(indexName: "user_institution_on_institution_id_idx", tableName: "user_institution") {
			column(name: "institution_id")
		}

		createIndex(indexName: "user_institution_on_user_id_idx", tableName: "user_institution") {
			column(name: "user_id")
		}

		createIndex(indexName: "user_problem_on_problem_id_idx", tableName: "user_problem") {
			column(name: "problem_id")
		}

		createIndex(indexName: "user_problem_on_user_id_idx", tableName: "user_problem") {
			column(name: "user_id")
		}

		createIndex(indexName: "user_role_on_role_id_idx", tableName: "user_role") {
			column(name: "role_id")
		}

		createIndex(indexName: "user_role_on_user_id_idx", tableName: "user_role") {
			column(name: "user_id")
		}
		
	}

}