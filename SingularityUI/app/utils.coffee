class Utils

    getHTMLTitleFromHistoryFragment: (fragment) ->
        _.capitalize(fragment.split('\/').join(' '))

    stringJSON: (object) ->
        JSON.stringify object, null, '    '

    viewJSON: (type, objectId) ->
        lookupObject = {}

        if type is 'task'
            lookupObject = app.allTasks
        if type is 'request'
            lookupObject = app.allRequests

        vex.dialog.alert
            contentCSS:
                width: 800
            message: "<pre>#{ lookupObject[objectId].JSONString }</pre>"

    setupSortableTables: ->
        sortable.init()

    paramsString: (params) ->
        paramsString = ''
        for key, value of params
            paramsString += "#{ key }=#{ value }&"
        paramsString

module.exports = new Utils