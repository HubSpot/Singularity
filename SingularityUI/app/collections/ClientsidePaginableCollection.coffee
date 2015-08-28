PaginableCollection = require './PaginableCollection'

# A base collection used for client-side pagination
class ClientsidePaginableCollection extends PaginableCollection

    isPaginated: false
    
    setPaginatedCollection: ->
        @isPaginated = true
        @paginatedCollection = []
        collection = @toJSON()

        # break the collection up into chunks that
        # will be accessed by their index as a `page`
        # in `paginatedTableClientsideView`
        while collection.length
            @paginatedCollection.push collection.splice 0, @atATime

        @totalPages = @paginatedCollection.length

    getPaginatedCollection: ->
        @setPaginatedCollection() unless @isPaginated
        @paginatedCollection[ @currentPage - 1 ] || []


module.exports = ClientsidePaginableCollection