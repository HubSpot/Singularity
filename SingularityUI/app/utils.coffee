class Utils

    @getHTMLTitleFromHistoryFragment: (fragment) ->
        _.capitalize(fragment.split('\/').join(' '))

    @getShortTaskID: (taskId) ->
        split = taskId.split(/\-|\:|\./)
        if split.length > 1
            return "#{ split[0] }..."
        else
            return _.truncate(taskId, 20)
        return taskId

    @getShortRequestID: (requestId) ->
        split = requestId.split(/\-|\:|\./)
        if split.length > 1
            return "#{ split[0] }"
        else
            return _.truncate(requestId, 20)
        return requestId

    @getShortTaskIDMiddleEllipsis: (taskId) ->
        dateRegExp = /\-\d{12,}\-/
        bigSplit = taskId.split dateRegExp

        if bigSplit.length is 2
            "#{ utils.getShortTaskID(bigSplit[0]) }#{ taskId.match(dateRegExp)[0] }#{ _.truncate(bigSplit[1], 25) }"
        else
            utils.getShortTaskID taskId

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
            message: "<pre>#{ utils.htmlEncode lookupObject[objectId].JSONString }</pre>"

    @htmlEncode: (value) ->
        $('<div>').text(value).html()

    @setupSortableTables: ->
        sortable.init()

    @humanTime: (date, future = false) ->
        return '' unless date?
        now = moment()
        time = moment(date)
        wasToday = time.date() is now.date() and Math.abs(time.diff(now)) < 86400000
        wasJustNow = Math.abs(time.diff(now)) < 120000
        """#{ if future then time.from() else time.fromNow() } #{ if wasJustNow then '' else time.format('(' + (if wasToday then '' else 'l ') + 'h:mma)') }"""

    @humanTimeAgo: (date) ->
        utils.humanTime date

    @humanTimeSoon: (date) ->
        utils.humanTime date, future = true

    @flashRow: ($row) ->
        $row.removeClass('flash-prime flash')
        $row.addClass('flash-prime')
        $row[0].clientHeight
        $row.addClass('flash')

module.exports = Utils