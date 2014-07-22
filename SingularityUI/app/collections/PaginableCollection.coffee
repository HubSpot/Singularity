Collection = require './collection'

# Another base collection used for stuff that can be server-paginated
# Used in stuff that's in ExpandableTableSubview
class PaginableCollection extends Mixen(Backbone.Collection, Collection)

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
