ExpandableTableSubview = require './expandableTableSubview'

# Reusable view for client-side paginable tables
# Used together with expandableTableSubview which
# does most of the heavy lifting
#
# You feed it a collection and a template 
# and it works its magic

class PaginatedTableClientsideView extends ExpandableTableSubview

    getRenderData: ->
        @collection.getPaginatedCollection() 

    checkCollectionLength: ->
        return not (@collection.getPaginatedCollection().length isnt @collection.atATime and not @haveButtons)

    checkHasNextButton: ->
        @collection.currentPage isnt @collection.totalPages

    loadNextPage: ->
        @collection.currentPage += 1 unless @collection.getPaginatedCollection().length isnt @collection.atATime
        @render()

    loadPreviousPage: ->
        @render() 

    refreshCollection: ->        
        @collection.setPaginatedCollection()
        @render()

module.exports = PaginatedTableClientsideView
