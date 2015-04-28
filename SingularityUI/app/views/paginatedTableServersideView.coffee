ExpandableTableSubview = require './expandableTableSubview'

# Reusable view for server-side paginable tables
# Used together with expandableTableSubview which
# does most of the heavy lifting
#
# You feed it a server-side paginated collection
# and a template and it works its magic


class PaginatedTableServersideView extends ExpandableTableSubview

    getRenderData: ->
        @collection.toJSON()

    checkCollectionLength: ->
        not (@collection.length isnt @collection.atATime and not @haveButtons)

    checkHasNextButton: ->
        @collection.length is @collection.atATime

    loadNextPage: ->
        @collection.currentPage += 1 unless @collection.length isnt @collection.atATime
        @refreshCollection()

    loadPreviousPage: ->
        @refreshCollection()

    refreshCollection: ->
        @collection.fetch()

module.exports = PaginatedTableServersideView