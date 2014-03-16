class BufferWrites

    @pendingCreates: []

    sync: (method, model, options) ->
        if method is 'create'
            # Let's buffer all the creates we get in the same tick, so we
            # can save them with a single request
            deferred = $.Deferred()
            BufferWrites.pendingCreates.push {model, options, deferred}
            setTimeout _.bind(@flush, @), 0
            deferred.promise().then(options.success, options.error)
        else
            super

    flush: ->
        return unless BufferWrites.pendingCreates.length

        creates = BufferWrites.pendingCreates

        $.ajax _.extend creates[0].options,
            url: _.result(@, 'url')
            type: 'POST'
            contentType: 'application/json'
            data: JSON.stringify _.invoke(_.pluck(creates, 'model'), 'toJSON')
            success: =>
                for {options, deferred} in creates
                    deferred.resolve(arguments...)
            error: =>
                for {options, deferred} in creates
                    deferred.reject(arguments...)

        BufferWrites.pendingCreates = []

module.exports = BufferWrites
