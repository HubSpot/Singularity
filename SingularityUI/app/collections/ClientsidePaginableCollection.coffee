PaginableCollection = require './PaginableCollection'

# A base collection used for client-side pagination
class ClientsidePaginableCollection extends PaginableCollection

    isPaginated: false
    
    setPaginatedCollection: ->
        @isPaginated = true
        @paginatedCollection = []
        collection = @toJSON()

        while collection.length
            @paginatedCollection.push collection.splice 0, @atATime

    getPaginatedCollection: ->
        @setPaginatedCollection() unless @isPaginated
        @paginatedCollection[ @currentPage - 1 ] || []

module.exports = ClientsidePaginableCollection