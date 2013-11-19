class Utils

    getHTMLTitleFromHistoryFragment: (fragment) ->
        _.capitalize(fragment.split('\/').join(' '))

    stringJSON: (object) ->
        JSON.stringify object, null, '    '

    viewJSON: (object) ->
        objectClone = _.extend {}, object
        delete objectClone.JSONString
        vex.dialog.alert
            contentCSS:
                width: 800
            message: "<pre>#{ utils.stringJSON objectClone }</pre>"

    getAcrossCollections: (collectionStrings, id) ->
        model = undefined
        _.each collectionStrings, (collectionString) ->
            collection = app.collections[collectionString]
            model = collection.get(id) ? model
        return model

    setupSortableTables: ->
        $('table.sortTable').each -> sorttable.makeSortable @

module.exports = new Utils