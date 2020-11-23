package com.thehuxley

import groovy.transform.EqualsAndHashCode
import org.springframework.context.i18n.LocaleContextHolder

@EqualsAndHashCode(includes = 'id')
class User implements Serializable {

    def springSecurityService

    String username
    String password
    Boolean enabled = true
    Boolean accountExpired = false
    Boolean accountLocked = false
    Boolean passwordExpired = false
    String email
    String name
    Date dateCreated
    Date lastUpdated
    String avatar = "default.png"

    String locale

    static hasOne = [topCoder: TopCoder]

    static hasMany = [roles: Role, userGroups: UserGroup, submissions: Submission, userInstitutions: UserInstitution]

    static belongsTo = [institution: Institution]

    static searchable = {
        only = ["username", "name", "email"]
    }

    static transients = ["springSecurityService"]

    static mapping = {
        id generator: "sequence", params: [sequence: "user_id_seq"]
        roles joinTable: [name: "user_role", column: "role_id", key: "user_id"]
    }

    static constraints = {
        username blank: false, matches: "[a-zA-Z0-9]+", size: 1..255, nullable: false, validator: { val, obj ->
            def currentUser = User.findByUsernameIlike(val)
            return !currentUser || obj.id == currentUser.id
        }
        password nullable: false, blank: false, size: 6..255
        email nullable: false, blank: false, email: true, unique: true, validator: { val, obj ->
            def currentUser = User.findByEmailIlike(val)
            return !currentUser || obj.id == currentUser.id
        }
        name blank: false, nullable: false
        dateCreated nullable: true
        lastUpdated nullable: true
        avatar nullable: true
        institution nullable: true, lazy: false
        topCoder nullable: true
        roles lazy: false
    }

    Set<Role> getAuthorities() {
        roles
    }

    Boolean hasRole(role) {
        roles.contains(role)
    }

    def beforeInsert() {
        encodePassword()
    }

    def beforeUpdate() {
        if (isDirty("password")) {
            encodePassword()
        }
    }

    protected void encodePassword() {
        password = springSecurityService?.passwordEncoder ? springSecurityService.encodePassword(password) : password
    }

}
