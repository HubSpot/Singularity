class Utils

    getHTMLTitleFromHistoryFragment: (fragment) ->
        _.capitalize(fragment.split('\/').join(' '))

    stringJSON: (object) ->
        JSON.stringify object, null, '    '

    viewJSON: (object) ->
        vex.dialog.alert
            contentCSS:
                width: 800
            message: "<pre>#{ utils.stringJSON object }</pre>"

    getAcrossCollections: (collectionStrings, id) ->
        model = undefined
        _.each collectionStrings, (collectionString) ->
            collection = app.collections[collectionString]
            model = collection.get(id) ? model
        return model

module.exports = new Utils