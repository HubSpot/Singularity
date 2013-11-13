class Utils

    getHTMLTitleFromHistoryFragment: (fragment) ->
        _.capitalize(fragment.split('\/').join(' '))

    viewJSON: (object) ->
        vex.dialog.alert
            contentCSS:
                width: 800
            message: "<pre>#{ JSON.stringify object, null, '    ' }</pre>"

    getAcrossCollections: (collections, id) ->
        model = undefined
        _.each collections, (collection) ->
            model = collection.get(id) ? model
        return model

module.exports = new Utils