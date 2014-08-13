Collection = require './collection'

# Another base collection used for stuff that can be server-paginated
# Used in stuff that's in ExpandableTableSubview
class PaginableCollection extends Backbone.Collection

    # Tracks if the collection has synced
    synced: false

    constructor: ->
        super
        @on 'sync',  =>
            @synced = true
            @each (model) => model.synced = true
        @on 'reset', => @synced = false

    # End of stuff from Collection

    atATime: 5
    currentPage: 1

    fetch: (params = {}) =>
        defaultData =
            count: @atATime
            page:  @currentPage

        defaultParams =
            reset: true
            data: _.extend defaultData, params.data

        super _.extend params, defaultParams

module.exports = PaginableCollection
