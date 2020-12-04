node {

    def allApplicationStatus = ""

    //APPS and ENVIRONMENTS are parameters defined in jenkin pipeline job
    stage("Status Check") {
        steps {
            def env1Result = executeCurl(getScript("testUser", "name.node1"))
            def result = formatToHtmlReport(env1Result)
            println result
            allApplicationStatus += result
        }
    }

    //EMAIL_ID_LIST is a jenkin parameter
    stage("Send Email") {
        emailtext(
                subject: "[Project Name] [Env] APPLICATION STATUS",
                body: """
                    ${allApplicationStatus}
                    <p> Check console log at &QUOT;<a href='$env.BUILD_URL'> ${env.JOB_NAME}
                    ${env.BUILD_NUMBER}
                    """,
                to: EMAIL_ID_LIST
        )
    }

}

def getScript(String user, String nameNode) {
    return "curl 'http://${nameNode}:8088/cluster/apps/RUNNING|grep href|grep ${user}'"
}

def getVersion(def env) {
    return "3.4.5" // get version logic
}

def executeCurl(def script) {
    try {
        sh "${script} > appStatus.txt"
        sh "cat appStatus.txt|gawk -F '\",\"' '{print \$3,\$9}' > out.txt"
        return readFile("out.txt")
    } catch (any) {
        return ""
    }
}

def formatToHtmlReport(Map result) {
//    APPS = "app1,app2"
//    ENVIRONMENTS = ["Env1", "Env2"]

    def listOfApps = APPS.replaceAll("\"", "").split(',')
    def envNames = ENVIRONMENTS.split(',') as List

    def htmlReport = "<table border=\"1\">\n<thead>\n"
    htmlReport += "<th bgcolor=\">"

    or(environment in envNames) {
        def version = getVersion(environment)
        htmlReport += "<th bgcolor=\"#808080\" align=\"center\"${environment} <br>${version}</br></th>"
    }
    htmlReport += "</tr>"
    htmlReport += "</thead>\n"
    htmlReport += "<tbody>"
    htmlReport += "</tr>\n</thead>"
    def status = ""
    for (app in listOfApps) {
        htmlReport += "<tr>\n"
        htmlReport += "<td bgcolor=\"#cecece\" align=\"left\"><p style=\"color:black;\">${app}</p></td>\n"

        for (environment in envNames) {
            status = getStatus(result, environment, app)
            htmlReport += status
        }
        htmlReport += "</tr>\n"
    }
    htmlReport += "</tbody>\n"
    htmlReport += "</table>\n"
    htmlReport += "</tbody>\n</table>\n"

    return htmlReport
}

def getStatus(result, environment, app) {
    return result.getAt(app + environment)
}