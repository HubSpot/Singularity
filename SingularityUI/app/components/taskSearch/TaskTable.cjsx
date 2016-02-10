StripedTable = require '../common/Table'

TaskTable = React.createClass

    render: ->
        taskTableColumns = ["Name", "Last State", "Deploy", "Started", "Updated"]
        taskTableData = []
        for task in @props.models
            taskTableData.push([
                {
                    component:'link'
                    text: task.taskId.id
                    url: "#{window.config.appRoot}/task/#{task.taskId.id}"
                    altText: task.taskId.id
                },
                {
                    component: 'taskStateLabel'
                    taskState: task.lastTaskState
                },
                {
                    component: 'link'
                    text: task.taskId.deployId
                    url: "#{window.config.appRoot}/request/#{task.taskId.requestId}/deploy/#{task.taskId.deployId}"
                    altText: task.taskId.deployId
                },
                {
                    component: 'timestamp'
                    timestamp: task.taskId.startedAt
                    display: 'timeStampFromNow' 
                },
                {
                    component: 'timestamp'
                    timestamp: task.updatedAt
                    display: 'timeStampFromNow'
                }
            ])
        return <StripedTable
                    tableClassOpts="table-striped"
                    columnNames={taskTableColumns}
                    tableRows={taskTableData}
                />

module.exports = TaskTable