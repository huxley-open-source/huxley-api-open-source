
class BootStrap {

    def rabbitContext
    def init = { servletContext ->

        try {
            println rabbitContext.getClass()
            rabbitContext.load()
            rabbitContext.start()
        } catch (Exception ex) {
            ex.printStackTrace()
        }
    }

    def destroy = {
    }

}
