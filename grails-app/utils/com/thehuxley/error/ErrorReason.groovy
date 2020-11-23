package com.thehuxley.error

enum ErrorReason {


	WRONG_ORDER_PARAM(2000, "Wrong value for order, try 'asc' or 'desc'."),

	WRONG_SORT_PARAM(2001, "Wrong value for sort."),

	ENTITY_NOT_FOUND(4000, "Entity not found."),

	USER_EMAIL_MUST_BE_UNIQUE(301021, "{0} is already in use. email must be unique."),

	USER_EMAIL_IS_NOT_VALID(301022, "{0} is not a valid e-mail address."),

	USER_EMAIL_CANNOT_BE_NULL(301023, "email cannot be null."),

	USER_EMAIL_CANNOT_BE_BLANK(301024, "email cannot be blank."),

	USER_USERNAME_MUST_BE_UNIQUE(301011, "{0} is already in use. username must be unique."),

	USER_USERNAME_NOT_MATCH(301012, "{0} does not match the required pattern [a-zA-Z0-9]+."),

	USER_USERNAME_TOO_BIG(301013, "{0} does not fall within the valid size range from 1 to 255."),

	USER_USERNAME_TOO_SMALL(301014, "{0} does not fall within the valid size range from 1 to 255."),

	USER_USERNAME_CANNOT_BE_BLANK(301015, "username cannot be blank."),

	USER_USERNAME_CANNOT_BE_NULL(301016, "username cannot be null."),

	USER_PASSWORD_CANNOT_BE_NULL(301034, 'password cannot be null.'),

	USER_PASSWORD_CANNOT_BE_BLANK(301035, 'password cannot be blank.'),

	USER_PASSWORD_WRONG(301031, "wrong password."),

	USER_PASSWORD_INVALID(301032, "password does not fall within the valid size range from 6 to 255."),

	USER_PASSWORD_NOT_MATCH(301033, "newPassword and confirmNewPassword does not match."),

	AVATAR_INVALID_SIZE(301041, "avatar does not fall within the valid size from 1KB to 5MB"),

	AVATAR_INVALID_MIME_TYPE(301042, "Only jpg and png are allowed."),

	USER_NAME_CANNOT_BE_NULL(301051, "name cannot be null."),

	USER_NAME_CANNOT_BE_BLANK(301052, "name cannot be blank."),

	GROUP_NAME_MUST_BE_UNIQUE(302011, "{0} is already in use. name must be unique."),

	GROUP_NAME_TOO_BIG(302013, "{0} does not fall within the valid size range from 1 to 255."),

	GROUP_NAME_TOO_SMALL(302014, "{0} does not fall within the valid size range from 1 to 255."),

	GROUP_NAME_CANNOT_BE_BLANK(302015, "name cannot be blank."),

	GROUP_NAME_CANNOT_BE_NULL(302016, "institution cannot be null."),

	GROUP_URL_MUST_BE_UNIQUE(302021, "{0} is already in use. url must be unique."),

	GROUP_URL_NOT_MATCH(302022, "{0} does not match the required pattern [a-zA-Z0-9-]+."),

	GROUP_URL_TOO_BIG(302023, "{0} does not fall within the valid size range from 1 to 255."),

	GROUP_URL_TOO_SMALL(302024, "{0} does not fall within the valid size range from 1 to 255."),

	GROUP_URL_CANNOT_BE_BLANK(302025, "url cannot be blank."),

	GROUP_URL_CANNOT_BE_NULL(302026, "url cannot be null."),

	GROUP_INSTITUTION_CANNOT_BE_NULL(302031, "institution cannot be null."),

	GROUP_START_DATE_CANNOT_BE_NULL(302041, "startDate cannot be null."),

	GROUP_END_DATE_CANNOT_BE_NULL(302051, "endDate cannot be null."),

	GROUP_END_DATE_CANNOT_BE_EARLIER_THAN_START_DATA(302052, "endDate cannot be earlier than startDate."),


	PROBLEM_NAME_MUST_BE_UNIQUE(303011, "{0} is already in use. name must be unique."),

	PROBLEM_NAME_TOO_BIG(303012, "{0} does not fall within the valid size range from 1 to 255."),

	PROBLEM_NAME_TOO_SMALL(303013, "{0} does not fall within the valid size range from 1 to 255."),

	PROBLEM_NAME_CANNOT_BE_BLANK(303014, "name cannot be blank."),

	PROBLEM_NAME_CANNOT_BE_NULL(303015, "name cannot be null."),

	PROBLEM_LEVEL_OUT_OF_RANGE(303021, "level out of the range [1..10]."),

	PROBLEM_IMAGE_INVALID_SIZE(301041, "image does not fall within the valid size from 1KB to 5MB."),

	PROBLEM_IMAGE_INVALID_MIME_TYPE(301042, "Only jpg and png are allowed."),


	INSTITUTION_NAME_MUST_BE_UNIQUE(304011, "{0} is already in use. name must be unique."),

	INSTITUTION_NAME_TOO_BIG(304012, "{0} does not fall within the valid size range from 1 to 255."),

	INSTITUTION_NAME_TOO_SMALL(304013, "{0} does not fall within the valid size range from 1 to 255."),

	INSTITUTION_NAME_CANNOT_BE_BLANK(304014, "name cannot be blank"),

	INSTITUTION_NAME_CANNOT_BE_NULL(304015, "name cannot be null."),

	INSTITUTION_ACRONYM_MUST_BE_UNIQUE(304021, "{0} is already in use. acronym must be unique."),

	INSTITUTION_ACRONYM_TOO_BIG(304022, "{0} does not fall within the valid size range from 1 to 255."),

	INSTITUTION_ACRONYM_TOO_SMALL(304023, "{0} does not fall within the valid size range from 1 to 255."),

	INSTITUTION_ACRONYM_CANNOT_BE_BLANK(304024, "acronym cannot be blank"),

	INSTITUTION_ACRONYM_CANNOT_BE_NULL(304025, "acronym cannot be null."),

	LOGO_INVALID_SIZE(304031, "avatar does not fall within the valid size from 1KB to 5MB"),

	LOGO_INVALID_MIME_TYPE(304032, "Only jpg and png are allowed."),


	QUESTIONNAIRE_TITLE_CANNOT_BE_BLANK(305011, "title cannot be blank."),

	QUESTIONNAIRE_TITLE_TOO_BIG(305012, "{0} does not fall within the valid size range from 1 to 255."),

	QUESTIONNAIRE_TITLE_TOO_SMALL(305013, "{0} does not fall within the valid size range from 1 to 255."),

	QUESTIONNAIRE_TITLE_CANNOT_BE_NULL(305014, "title cannot be null."),

	QUESTIONNAIRE_DESCRIPTION_TOO_BIG(305021, "{0} does not fall within the valid size range from 1 to 255."),

	QUESTIONNAIRE_DESCRIPTION_TOO_SMALL(30502, "{0} does not fall within the valid size range from 1 to 255."),

	QUESTIONNAIRE_GROUP_CANNOT_BE_NULL(305031, "group cannot be null"),

	QUESTIONNAIRE_END_DATE_CANNOT_BE_NULL(305041, "endDate cannot be null."),

	QUESTIONNAIRE_START_DATE_CANNOT_BE_NULL(305051, "startDate cannot be null."),

	QUESTIONNAIRE_END_DATE_CANNOT_BE_EARLIER_THAN_START_DATA(305042, "endDate cannot be earlier than startDate."),


	TOPIC_NAME_CANNOT_BE_BLANK(306011, "name cannot be blank."),

	TOPIC_NAME_TOO_BIG(306012, "{0} does not fall within the valid size range from 1 to 255."),

	TOPIC_NAME_TOO_SMALL(306013, "{0} does not fall within the valid size range from 1 to 255."),

	TOPIC_NAME_CANNOT_BE_NULL(306014, "name cannot be null."),

	TOPIC_NAME_MUST_BE_UNIQUE(306015, "{0} is already in use. name must be unique."),


	LANGUAGE_NAME_CANNOT_BE_BLANK(307011, "name cannot be blank."),

	LANGUAGE_NAME_TOO_BIG(307012, "{0} does not fall within the valid size range from 1 to 255."),

	LANGUAGE_NAME_TOO_SMALL(307013, "{0} does not fall within the valid size range from 1 to 255."),

	LANGUAGE_NAME_CANNOT_BE_NULL(307014, "name cannot be null."),

	LANGUAGE_NAME_MUST_BE_UNIQUE(307015, "{0} is already in use. name must be unique."),

	LANGUAGE_COMPILE_PARAMS_TOO_BIG(307031, "{0} does not fall within the valid size range from 1 to 255."),

	LANGUAGE_COMPILE_PARAMS_TOO_SMALL(307032, "{0} does not fall within the valid size range from 1 to 255."),

	LANGUAGE_COMPILER_CANNOT_BE_BLANK(307041, "compiler cannot be blank."),

	LANGUAGE_COMPILER_TOO_BIG(307042, "{0} does not fall within the valid size range from 1 to 255."),

	LANGUAGE_COMPILER_TOO_SMALL(307043, "{0} does not fall within the valid size range from 1 to 255."),

	LANGUAGE_COMPILER_CANNOT_BE_NULL(307044, "compiler cannot be null."),

	LANGUAGE_EXEC_PARAMS_TOO_BIG(307051, "{0} does not fall within the valid size range from 1 to 255."),

	LANGUAGE_EXEC_PARAMS_TOO_SMALL(307052, "{0} does not fall within the valid size range from 1 to 255."),

	LANGUAGE_SCRIPT_CANNOT_BE_BLANK(307061, "script cannot be blank."),

	LANGUAGE_SCRIPT_TOO_BIG(307062, "{0} does not fall within the valid size range from 1 to 255."),

	LANGUAGE_SCRIPT_TOO_SMALL(307063, "{0} does not fall within the valid size range from 1 to 255."),

	LANGUAGE_SCRIPT_CANNOT_BE_NULL(307064, "script cannot be null."),

	LANGUAGE_EXTENSION_CANNOT_BE_BLANK(307071, "extension cannot be blank."),

	LANGUAGE_EXTENSION_TOO_BIG(307072, "{0} does not fall within the valid size range from 1 to 255."),

	LANGUAGE_EXTENSION_TOO_SMALL(307073, "{0} does not fall within the valid size range from 1 to 255."),

	LANGUAGE_EXTENSION_CANNOT_BE_NULL(307074, "extension cannot be null."),

	LANGUAGE_LABEL_CANNOT_BE_BLANK(307081, "label cannot be blank."),

	LANGUAGE_LABEL_TOO_BIG(307082, "{0} does not fall within the valid size range from 1 to 255."),

	LANGUAGE_LABEL_TOO_SMALL(307083, "{0} does not fall within the valid size range from 1 to 255."),

	LANGUAGE_LABEL_CANNOT_BE_NULL(307084, "label cannot be null."),


	GENERIC (0, '{0}')


	private int value
	private final String reason

	List params

	private ErrorReason(int value, String reasonPhrase) {
		this.value = value;
		this.reason = reasonPhrase;
	}

	def setParams(Object ... params) {
		this.params = params

		return this
	}

}
