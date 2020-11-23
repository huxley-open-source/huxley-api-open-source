package com.thehuxley

import grails.web.servlet.mvc.GrailsParameterMap

class PendencyService {

    def get(Long pendencyId) {
        Pendency.get(pendencyId)
    }

    def list(Map params) {
        Pendency.createCriteria().list(params) {
            and {

                or {
                    if (params.q) {
                        institution {
                            ilike("name", "%$params.q%")
                        }

                        group {
                            ilike("name", "%$params.q%")
                        }

                        user {
                            ilike("name", "%$params.q%")
                        }
                    }
                }

                if (params.kind) {
                    eq("kind", params.kind)
                }

                if (params.status) {
                    eq("status", params.status)
                }

                if (params.user) {
                    eq("user", User.load(params.user as Long))
                }

                if (params.institution) {
                    eq("institution", Institution.load(params.institution as Long))
                }

                if (params.group) {
                    eq("group", Group.load(params.group as Long))
                }
            }

            order(params.sort ?: "status", params.order ?: "asc")
        }
    }

    def save(Pendency pendency) {
        pendency.save()
    }

    GrailsParameterMap normalize(GrailsParameterMap params) {
        params.q = params.q ?: ""
        params.max = Math.min(params.int("max", 0) ?: 10, 100)
        params.offset = params.int("offset", 0)
        params.kind = params.kind ? Pendency.PendencyKind.valueOf(params.kind as String) : null
        params.status = params.status ? Pendency.Status.valueOf(params.status as String) : null
        params.institution = params.institution ? params.long("institution") : null
        params.group = params.group ? params.long("group") : null
        params.user = params.user ? params.long("user") : null

        return params
    }

    boolean isSortable(param) {
        [
                "id",
                "status",
                "institution",
                "dateCreated",
                "lastUpdated",
                "status"
        ].contains(param)
    }
}