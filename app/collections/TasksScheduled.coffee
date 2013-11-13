Collection = require './collection'

Tasks = require './Tasks'

MOCK_JSON = [{"request":{"command":null,"name":"hspypi_web-purge","executor":"/home/tpetr/mesos/bin/hubspot-mesos-executor","resources":{"cpus":1,"memoryMb":512,"numPorts":0},"schedule":"0 30 0 ? * Sat","instances":1,"daemon":false,"env":null,"uris":null,"executorData":{"cmd":"django-admin.py purge_old_releases --settings hspypi_web.settings","uris":["http://hubspot.com.s3.amazonaws.com/build_artifacts/hspypi_web/hspypi_web-316-2013-11-10_13-36-37.tar.gz?AWSAccessKeyId=1JBPVR3ZRKYRTDP6S4G2&Expires=1415726274&Signature=1o9P4FBQw6tCFwBKOkaMeLUD3ko%3D","http://hubspot.deploy.config.s3.amazonaws.com/hspypi_web/316/config.yaml?AWSAccessKeyId=1JBPVR3ZRKYRTDP6S4G2&Expires=1415726273&Signature=xC7eRb6mgWT4t9ewVTX685yfxJs%3D"],"env":{"DEPLOY_PROJECT_NAME":"hspypi_web","DEPLOY_USER":"tpetr@hubspot.com","DEPLOY_ID":"e605113e-3fcd-485c-9f9a-5be32a20bf53","DEPLOY_TIME":"1384190274057"}},"rackSensitive":null},"pendingTaskId":{"name":"hspypi_web-purge","nextRunAt":1384561800000,"instanceNo":1}}]

class TasksScheduled extends Tasks

    #url: "http://#{env.SINGULARITY_BASE}/#{constants.api_base}/task/active"
    url: => "https://#{env.INTERNAL_BASE}/#{constants.kumonga_api_base}/users/#{app.login.context.user.email}/settings"

    parse: (tasks) ->
        tasks = MOCK_JSON

        _.each tasks, (task, i) =>
            task.id = @parsePendingId task.pendingTaskId
            task.name = task.id
            task.label = @parseLabelFromName task.id
            task.nextRunAt = task.pendingTaskId.nextRunAt
            task.nextRunAtHuman = moment(task.nextRunAt).fromNow()
            task.schedule = task.request.schedule
            tasks[i] = task

        tasks

    parsePendingId: (pendingTaskId) ->
        "#{ pendingTaskId.name }-#{ pendingTaskId.nextRunAt }-#{ pendingTaskId.instanceNo }"

    comparator: 'nextRunAt'

module.exports = TasksScheduled