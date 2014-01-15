class Utils

    @getHTMLTitleFromHistoryFragment: (fragment) ->
        _.capitalize(fragment.split('\/').join(' '))

    @getShortTaskID: (taskId) ->
        split = taskId.split(/\-|\:|\./)
        if split.length > 1
            return "#{ split[0] }..."
        else
            if taskId.length > 20
                return "#{ taskId.substr(0, 20) }..."
        return taskId

    @stringJSON: (object) ->
        JSON.stringify object, null, '    '

    @viewJSON: (type, objectId) ->
        lookupObject = {}

        if type is 'task'
            lookupObject = app.allTasks
        if type is 'request'
            lookupObject = app.allRequests

        vex.dialog.alert
            contentCSS:
                width: 800
            message: "<pre>#{ lookupObject[objectId].JSONString }</pre>"

    @setupSortableTables: ->
        sortable.init()

    @humanTimeAgo: (date) ->
        return '' unless date?
        now = moment()
        time = moment(date)
        wasToday = time.date() is now.date() and Math.abs(time.diff(now)) < 86400000
        wasJustNow = Math.abs(time.diff(now)) < 120000
        """#{ time.from() } #{ if wasJustNow then '' else time.format('(' + (if wasToday then '' else 'l ') + 'h:mma)') }"""

module.exports = Utils