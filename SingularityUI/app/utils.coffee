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

    getAcrossCollections: (collectionStrings, id) ->
        model = undefined
        _.each collectionStrings, (collectionString) ->
            collection = app.collections[collectionString]
            model = collection.get(id) ? model
        return model

    setupSortableTables: ->
        $('table[data-sortable="true"]').each -> SorTable.init @

module.exports = new Utils