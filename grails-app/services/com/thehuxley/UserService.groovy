package com.thehuxley

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import com.googlecode.pngtastic.core.PngImage
import com.googlecode.pngtastic.core.PngOptimizer
import com.thehuxley.predictor.ClusteringPredictor
import com.thehuxley.predictor.FailingPredictable
import com.thehuxley.predictor.Parameter
import com.thehuxley.predictor.Student
import grails.web.servlet.mvc.GrailsParameterMap
import net.coobird.thumbnailator.Thumbnails
import org.hibernate.FetchMode
import org.hibernate.SessionFactory
import org.hibernate.criterion.CriteriaSpecification
import org.hibernate.sql.JoinType
import org.hibernate.transform.Transformers
import org.springframework.security.crypto.codec.Hex
import org.springframework.web.multipart.MultipartFile

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class UserService {

    def grailsApplication
    final Cache<String, User> usersCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build()

    SessionFactory sessionFactory


    def get(Long id) {
        User.createCriteria().get {
            createAlias('institution', 'i')
            createAlias('topCoder', 't')
            eq('id', id)
        }
    }

    def findByUsername(String username) {
        def result = usersCache.getIfPresent(username)

        if (!result) {
            result = User.findByUsername(username)
            if (result) {
                result.roles.forEach({}) // garante eager load
                usersCache.put(username, result)
            }
        }

        return result;
    }

    def uploadAvatar(MultipartFile file) {

        String path = grailsApplication.config.huxleyFileSystem.profile.images.dir + System.getProperty("file.separator")

        File dir = new File(path)
        dir.mkdirs()

        def originalFilename = file.originalFilename
        def index = originalFilename.lastIndexOf('.')
        def extension = ""
        if ((index > 0) && (originalFilename.size() > index)) {
            extension = originalFilename.substring(index - 1)
        }

        def filename = new String(Hex.encode(MessageDigest.getInstance("SHA1").digest(file.bytes))) + extension
        def destFile = new File(dir, filename)

        file.transferTo(destFile)

        return destFile
    }

    def crop(User user, String filename, Integer x, Integer y, Integer width, Integer height) {

        String path = grailsApplication.config.huxleyFileSystem.profile.images.dir + System.getProperty("file.separator")


        File normalDir = Paths.get(path, "avatar").toFile()
        File thumbsDir = new File(normalDir, "thumbs")
        normalDir.mkdirs()
        thumbsDir.mkdirs()


        def file = new File(path, filename)
        BufferedImage image = ImageIO.read(file)

        if (!image) {
            log.warn("file-not-found: ${path} / ${filename}")
            return null
        }

        image = image.getSubimage(x, y, width, height)

        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        baos.flush()

        def newFilename = new String(Hex.encode(MessageDigest.getInstance("SHA1").digest(baos.toByteArray()))) + ".png"

        ImageIO.write(image, "png", new File(path, newFilename))
        saveResized(image, 330, 330, new File(normalDir, newFilename))
        saveResized(image, 40, 40, new File(thumbsDir, newFilename))


        usersCache.invalidate(user.getUsername())

        if (user.id) {
            user = User.get(user.id)
        }

        user.avatar = newFilename
        user.save()

    }

    def getAvatar(String key, Integer width = 0, Integer height = 0) {        
        String path = grailsApplication.config.huxleyFileSystem.profile.images.dir + System.getProperty("file.separator")
        String temp = path + System.getProperty("file.separator") + "tmp" + System.getProperty("file.separator")

        def originalFile = new File(path, key)

        if (!originalFile.exists()) return null;

        BufferedImage avatar = ImageIO.read(originalFile)
        def resizedFile = new File(temp, originalFile.name)
        resizedFile.mkdirs()

        if ((width > 0) && !(height > 0)) {
            height = (3 / 4) * width
        }

        if ((height > 0) && !(width > 0)) {
            width = (4 / 3) * height
        }

        if (width > 0 && height > 0) {
            if (height == width) {
                def min = Math.min(avatar.width, avatar.height)
                avatar = avatar.getSubimage(
                        (avatar.width > avatar.height ? ((avatar.width - avatar.height) / 2).abs() : 0) as Integer,
                        0,
                        min,
                        min
                )
            }
            ImageIO.write(resizeImage(avatar, width, height), "png", resizedFile)

            return resizedFile
        }


        if (avatar) {
            ImageIO.write(avatar, "png", resizedFile)
            return resizedFile
        } else {
            log.warn("avatar-not-found: ${originalFile}")
            return null
        }

    }

    def saveResized(BufferedImage image, int width, int height, File dest) {
        ByteArrayOutputStream resized = new ByteArrayOutputStream()
        ImageIO.write(resizeImage(image, width, height), "png", resized)
        resized.flush()

        PngOptimizer optimizer = new PngOptimizer();
        PngImage pngImage = new PngImage(new ByteArrayInputStream(resized.toByteArray()))
        PngImage optimized = optimizer.optimize(pngImage, false, 9)

        ByteArrayOutputStream optimizedBytes = new ByteArrayOutputStream();
        optimized.writeDataOutputStream(optimizedBytes);
        optimized.export(dest.getAbsolutePath(), optimizedBytes.toByteArray());
    }

    def BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {

        return Thumbnails.of(originalImage).forceSize(width, height).asBufferedImage()
    }

    def save(User user) {

        log.info("save-user: { username: $user.username, email: $user.email }")

        user = user.merge(flush: true, failOnError: true)

        if (!user.getAuthorities() || user.getAuthorities().empty) {
            user.roles = []
            user.roles << Role.findByAuthority("ROLE_STUDENT")
            user = user.merge(failOnError: true)
        }

        usersCache.invalidate(user.getUsername())

        user
    }

    def generateRecoveryKey(User user) {
        new PendencyKey(type: PendencyKey.Type.CHANGE_PASSWORD, entity: user.id).save()
    }

    def list(Map params) {
        User.createCriteria().list([max: params.max, offset: params.offset], getCriteria(params))
    }

    def findAllInTopCoderWithFocus(User user, Integer max) {
        TopCoder.findAll(
                """FROM TopCoder t
                    INNER JOIN FETCH t.user as u
                    INNER JOIN FETCH u.institution i 
                    WHERE t.position > (SELECT position FROM TopCoder WHERE user_id = :user_id) - :max_users
					ORDER BY t.position ASC""",
                [user_id: user.id, max_users: (long) (max / 2)], [max: (long) max])
    }


    def findAllInTopCoder(Map params) {
        TopCoder.createCriteria().list([max: params.max, offset: params.offset]) {
            createAlias('user', 'user')
            createAlias('user.institution', 'institution')
            or {
                if (params.q) {
                    ilike("user.name", "%$params.q%")
                    ilike("user.email", "%$params.q%")
                    ilike("user.username", "%$params.q%")
                }
            }
            order("points", "desc")
        }
    }

    def findInTopCoder(User user) {
        TopCoder.findByUser(user)
    }

    def findByGroup(User user, Group group) {
        UserGroup.findByUserAndGroup(user, group)
    }

    def findAllByGroup(Group group, Map params) {
        User.createCriteria().list(params) {
            createAlias("userGroups", "ug")
            eq("ug.group", group)
            if (params.role) {
                eq("ug.role", UserGroup.Role.valueOf((params.role as String).toUpperCase()))
            }
        }
    }

    def refreshRoles(User user) {

        log.info("refresh-user-roles: $user.id")
        Role ADMIN_INST = Role.findByAuthority("ROLE_ADMIN_INST")
        Role TEACHER = Role.findByAuthority("ROLE_TEACHER")
        Role TEACHER_ASSISTANT = Role.findByAuthority("ROLE_TEACHER_ASSISTANT")

        def userInstitutions = UserInstitution.findAllByUserAndRole(user, UserInstitution.Role.ADMIN_INST)
        def hasApproved = false

        userInstitutions.each {
            if (it.institution.status == Institution.Status.APPROVED) {
                hasApproved = true
            }
        }

        if (hasApproved) {
            if (!user.hasRole(ADMIN_INST)) {
                user.roles << ADMIN_INST
                user.save()
            }
        } else {
            user.roles.remove(ADMIN_INST)
        }

        if (!UserInstitution.findAllByUserAndRole(user, UserInstitution.Role.TEACHER).empty) {
            if (!user.hasRole(TEACHER)) {
                user.roles << TEACHER
            }
        } else {
            user.roles.remove(TEACHER)
        }

        if (!UserInstitution.findAllByUserAndRole(user, UserInstitution.Role.TEACHER_ASSISTANT).empty) {
            if (user.hasRole(TEACHER_ASSISTANT)) {
                user.roles << TEACHER_ASSISTANT
            }
        } else {
            user.roles.remove(TEACHER_ASSISTANT)
        }

        usersCache.invalidate(user.getUsername())

        return true
    }

    def findAllByGroupAndRole(Group group, UserGroup.Role role, Map params) {
        User.createCriteria().list(params) {
            createAlias('userGroups', 'ug')
            eq("ug.group", group)
            eq("ug.role", role)
        }
    }

    def findByInstitution(User user, Institution institution) {
        UserInstitution userInstitution = UserInstitution.findByUserAndInstitution(user, institution)

        if (userInstitution) {
            userInstitution.user.metaClass.role = userInstitution.role

            userInstitution.user
        }
    }

    def findAllByInstitution(Institution institution, Map params) {

        params.role = params.role ?: null

        def resultList = User.createCriteria().list([max: params.max, offset: params.offset]) {
            createAlias('userInstitutions', 'ui')
            eq("ui.institution", institution)

            if (params.role) {

                def role = UserInstitution.Role.valueOf((params.role as String).toUpperCase())

                if (role == UserInstitution.Role.TEACHER) {
                    or {
                        eq("ui.role", UserInstitution.Role.TEACHER)
                        eq("ui.role", UserInstitution.Role.ADMIN_INST)
                    }
                } else {
                    eq("ui.role", role)
                }
            }
            and getCriteria(params)
        }

        resultList.each {
            it.metaClass.role = params.role
        }
    }

    def findAllByQuestionnaire(Questionnaire questionnaire, Map params) {
        UserGroup.createCriteria().list() {
            createAlias("user", "user")
            createAlias("user.topCoder", "topcoder", JoinType.LEFT_OUTER_JOIN)
            fetchMode("topcoder", FetchMode.JOIN)
            fetchMode("user.institution", FetchMode.JOIN)
            eq("group", questionnaire.group)
            eq("role", UserGroup.Role.STUDENT)
            order("user.name", "asc")
        }.user
    }

    def failingStudents(User user, Map params) {

        def sql = """SELECT u.id AS "userId",
                            g.id AS "groupId",
                            MAX(u.name) AS "userName",
                            MAX(g.name) AS "groupName",
                            MAX(g.url) AS "groupUrl",
                            COUNT(s.*) AS "submissionsCount",
                            SUM(CASE WHEN s.evaluation = :correctEval THEN 1 ELSE 0 END) AS "correctSubmissionsCount"
                      FROM user_group ug
                      JOIN public.group g ON ug.group_id = g.id  AND g.start_date < now() AND g.end_date > now()
                      JOIN public.user u ON u.id IN (SELECT user_id
                                                      FROM user_group ug2
                                                      WHERE ug2.group_id = g.id AND ug2.role = :studentRole)
                      JOIN submission s ON s.user_id = u.id AND s.submission_date > g.start_date AND s.submission_date < g.end_date
                      WHERE ug.user_id = :userId AND (role = :monitorRole OR role = :teacherRole)
                      GROUP BY u.id,g.id"""

        def result = sessionFactory.currentSession
                .createSQLQuery(sql)
                .setParameter("userId", user.id)
                .setParameter("studentRole", UserGroup.Role.STUDENT.ordinal())
                .setParameter("monitorRole", UserGroup.Role.TEACHER_ASSISTANT.ordinal())
                .setParameter("teacherRole", UserGroup.Role.TEACHER.ordinal())
                .setParameter("correctEval", Submission.Evaluation.CORRECT.ordinal())
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                .list()


        FailingPredictable predictor = new ClusteringPredictor()
        Multimap<Long, Student> studentsByGroup = ArrayListMultimap.create()

        def studentByIdAndGroup = [:]

        result.each {
            List<Parameter> parameters = []
            parameters.add(new Parameter("submissionsCount", it["submissionsCount"] as Double))
            parameters.add(new Parameter("correctSubmissionsCount", it["correctSubmissionsCount"] as Double))

            studentsByGroup.put(it.groupId, new Student(it.userId.longValue(), parameters))
            studentByIdAndGroup[it.groupId + '#' + it.userId] = it
        }

        def resultList = []

        studentsByGroup.keySet().each {
            def students = studentsByGroup.get(it)
            List<Student> failingStudents = predictor.filterStudentsLikelyToFail(students)

            failingStudents.each { Student st ->
                resultList.add(studentByIdAndGroup[it + '#' + st.id])
            }
        }

        return resultList

    }

    private Closure getCriteria(Map params) {
        return {
            createAlias('topCoder', 't', org.hibernate.sql.JoinType.LEFT_OUTER_JOIN)
            createAlias('institution', 'i', org.hibernate.sql.JoinType.LEFT_OUTER_JOIN)
            and {
                or {
                    if (params.q) {
                        ilike("name", "%$params.q%")
                        ilike("email", "%$params.q%")
                        ilike("username", "%$params.q%")
                    }
                }

                if (params.inUsers && !params.inUsers.empty)
                    inList("id", params.inUsers)
            }


            if (params.sort != "score") {
                order(params.sort ?: "name", params.order ?: "asc")
            }
        }
    }

    GrailsParameterMap normalize(GrailsParameterMap params) {
        params.max = Math.min(params.int("max", 0) ?: 10, 100)
        params.offset = params.int("offset", 0)
        params.q = params.q ?: ""

        return params
    }

    boolean isSortable(param) {
        [
                "id",
                "username",
                "accountLocked",
                "passwordExpired",
                "email",
                "name",
                "avatar",
                "dateCreated",
                "lastUpdated",
                "score"
        ].contains(param)
    }

}
