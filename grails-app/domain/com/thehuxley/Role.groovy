package com.thehuxley

import org.springframework.security.core.GrantedAuthority

class Role implements Serializable, GrantedAuthority {

    String authority

    static mapping = {
        id generator: "sequence", params: [sequence: "role_id_seq"]
    }

    static constraints = {
        authority blank: false, unique: true
    }

}
